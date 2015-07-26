package lithium;

public class Cell<A> {
	protected final Stream<A> event;
	A value;
	A valueUpdate;
	private Listener cleanup;
    protected Lazy<A> lazyInitValue;  // Used by lithium.LazyCell

	/**
	 * A cell with a constant value.
	 */
    public Cell(A value)
    {
    	this.event = new Stream<A>();
    	this.value = value;
    }

    Cell(final Stream<A> event, A initValue)
    {
    	this.event = event;
    	this.value = initValue;
    	Transaction.run(new Handler<Transaction>() {
    		public void run(Transaction trans1) {
	    		Cell.this.cleanup = event.listen(Node.NULL, trans1, new TransactionHandler<A>() {
	    			public void run(Transaction trans2, A a) {
			    		if (Cell.this.valueUpdate == null) {
			    			trans2.last(new Runnable() {
			    				public void run() {
				    				Cell.this.value = Cell.this.valueUpdate;
				    				Cell.this.lazyInitValue = null;
				    				Cell.this.valueUpdate = null;
				    			}
			    			});
			    		}
			    		Cell.this.valueUpdate = a;
			    	}
	    		}, false);
    		}
    	});
    }

    /**
     * @return The value including any updates that have happened in this transaction.
     */
    final A newValue()
    {
    	return valueUpdate == null ? sampleNoTrans() :  valueUpdate;
    }

    /**
     * Sample the cell's current value.
     *
     * This should generally be avoided in favour of value().listen(..) so you don't
     * miss any updates, but in many circumstances it makes sense.
     *
     * It can be best to use it inside an explicit transaction (using lithium.Transaction.run()).
     * For example, a b.sample() inside an explicit transaction along with a
     * b.updates().listen(..) will capture the current value and any updates without risk
     * of missing any in between.
     */
    public final A sample()
    {
        return Transaction.apply(new Lambda1<Transaction, A>() {
        	public A apply(Transaction trans) {
        		return sampleNoTrans();
        	}
        });
    }

    private static class LazySample<A> {
        LazySample(Cell<A> cell) {
            this.cell = cell;
        }
        Cell<A> cell;
        boolean hasValue;
        A value;
    }

    /**
     * A variant of sample() that works for CellLoops when they haven't been looped yet.
     */
    public final Lazy<A> sampleLazy() {
        final Cell<A> me = this;
        return Transaction.apply(new Lambda1<Transaction, Lazy<A>>() {
        	public Lazy<A> apply(Transaction trans) {
                final LazySample<A> s = new LazySample<A>(me);
                trans.last(new Runnable() {
                    @Override
                    public void run() {
                        s.value = me.valueUpdate != null ? me.valueUpdate : me.sampleNoTrans();
                        s.hasValue = true;
                        s.cell = null;
                    }
                });
                return new Lazy<A>(new Lambda0<A>() {
                    public A apply() {
                        if (s.hasValue)
                            return s.value;
                        else
                            return s.cell.sample();
                    }
                });
            }
        });
    }

    protected A sampleNoTrans()
    {
        return value;
    }

    /**
     * An event that gives the updates for the cell. If this cell was created
     * with a hold, then updates() gives you an event equivalent to the one that was held.
     */
    public final Stream<A> updates()
    {
    	return event;
    }

    /**
     * An event that is guaranteed to fire once when you listen to it, giving
     * the current value of the cell, and thereafter behaves like updates(),
     * firing for each update to the cell's value.
     */
    public final Stream<A> value()
    {
        return Transaction.apply(new Lambda1<Transaction, Stream<A>>() {
        	public Stream<A> apply(Transaction trans) {
        		return value(trans);
        	}
        });
    }

    final Stream<A> value(Transaction trans1)
    {
    	final StreamSink<Unit> sSpark = new StreamSink<Unit>();
        trans1.prioritized(sSpark.node, new Handler<Transaction>() {
            public void run(Transaction trans2) {
                sSpark.send(trans2, Unit.UNIT);
            }
        });
    	Stream<A> sInitial = sSpark.<A>snapshot(this);
        return sInitial.merge(updates()).lastFiringOnly(trans1);
    }

    /**
     * Transform the cell's value according to the supplied function.
     */
	public final <B> Cell<B> map(Lambda1<A,B> f)
	{
		return updates().map(f).holdLazy(sampleLazy().map(f));
	}

	/**
	 * Lift a binary function into cells.
	 */
	public static final <A,B,C> Cell<C> lift(final Lambda2<A,B,C> f, Cell<A> a, Cell<B> b)
	{
		Lambda1<A, Lambda1<B,C>> ffa = new Lambda1<A, Lambda1<B,C>>() {
			public Lambda1<B,C> apply(final A aa) {
				return new Lambda1<B,C>() {
					public C apply(B bb) {
						return f.apply(aa,bb);
					}
				};
			}
		};
		Cell<Lambda1<B,C>> bf = a.map(ffa);
		return apply(bf, b);
	}

	/**
	 * Lift a ternary function into cells.
	 */
	public static final <A,B,C,D> Cell<D> lift(final Lambda3<A,B,C,D> f, Cell<A> a, Cell<B> b, Cell<C> c)
	{
		Lambda1<A, Lambda1<B, Lambda1<C,D>>> ffa = new Lambda1<A, Lambda1<B, Lambda1<C,D>>>() {
			public Lambda1<B, Lambda1<C,D>> apply(final A aa) {
				return new Lambda1<B, Lambda1<C,D>>() {
					public Lambda1<C,D> apply(final B bb) {
						return new Lambda1<C,D>() {
							public D apply(C cc) {
								return f.apply(aa,bb,cc);
							}
						};
					}
				};
			}
		};
		Cell<Lambda1<B, Lambda1<C, D>>> bf = a.map(ffa);
		return a.apply(apply(bf, b), c);
	}

	/**
	 * Lift a quaternary function into cells.
	 */
	public static final <A,B,C,D,E> Cell<E> lift(final Lambda4<A,B,C,D,E> f, Cell<A> a, Cell<B> b, Cell<C> c, Cell<D> d)
	{
		Lambda1<A, Lambda1<B, Lambda1<C, Lambda1<D,E>>>> ffa = new Lambda1<A, Lambda1<B, Lambda1<C, Lambda1<D,E>>>>() {
			public Lambda1<B, Lambda1<C, Lambda1<D,E>>> apply(final A aa) {
				return new Lambda1<B, Lambda1<C, Lambda1<D,E>>>() {
					public Lambda1<C, Lambda1<D, E>> apply(final B bb) {
						return new Lambda1<C, Lambda1<D,E>>() {
							public Lambda1<D,E> apply(final C cc) {
                                return new Lambda1<D, E>() {
                                    public E apply(D dd) {
                                        return f.apply(aa,bb,cc,dd);
                                    }
                                };
							}
						};
					}
				};
			}
		};
		Cell<Lambda1<B, Lambda1<C, Lambda1<D, E>>>> bf = a.map(ffa);
		return apply(apply(apply(bf, b), c), d);
	}

	/**
	 * Apply a value inside a cell to a function inside a cell. This is the
	 * primitive for all function lifting.
	 */
	public static <A,B> Cell<B> apply(final Cell<Lambda1<A,B>> bf, final Cell<A> ba)
	{
    	return Transaction.apply(new Lambda1<Transaction, Cell<B>>() {
    		public Cell<B> apply(Transaction trans0) {
                final StreamSink<B> out = new StreamSink<B>();

                class ApplyHandler implements Handler<Transaction> {
                    ApplyHandler(Transaction trans0) {
                        resetFired(trans0);  // We suppress firing during the first transaction
                    }
                    boolean fired = true;
                    Lambda1<A,B> f = null;
                    A a = null;
                    @Override
                    public void run(Transaction trans1) {
                        if (fired) 
                            return;

                        fired = true;
                        trans1.prioritized(out.node, new Handler<Transaction>() {
                            public void run(Transaction trans2) {
                                out.send(trans2, f.apply(a));
                            }
                        });
                        resetFired(trans1);
                    }
                    void resetFired(Transaction trans1) {
                        trans1.last(new Runnable() {
                            @Override
                            public void run() {
                                fired = false;
                            }
                        });
                    }
                }

                Node out_target = out.node;
                final Node in_target = new Node(0);
                Node.Target[] node_target_ = new Node.Target[1];
                in_target.linkTo(null, out_target, node_target_);
                final Node.Target node_target = node_target_[0];
                final ApplyHandler h = new ApplyHandler(trans0);
                Listener l1 = bf.value().listen_(in_target, new TransactionHandler<Lambda1<A,B>>() {
                    public void run(Transaction trans1, Lambda1<A,B> f) {
                        h.f = f;
                        h.run(trans1);
                    }
                });
                Listener l2 = ba.value().listen_(in_target, new TransactionHandler<A>() {
                    public void run(Transaction trans1, A a) {
                        h.a = a;
                        h.run(trans1);
                    }
                });
                return out.unsafeAddCleanup(l1).unsafeAddCleanup(l2).unsafeAddCleanup(
                    new Listener() {
                        public void unlisten() {
                            in_target.unlinkTo(node_target);
                        }
                    }).holdLazy(new Lazy<B>(new Lambda0<B>() {
                        public B apply() {
                            return bf.sampleNoTrans().apply(ba.sampleNoTrans());
                        }
                    }));
            }
        });
	}

	/**
	 * Unwrap a cell inside another cell to give a time-varying cell implementation.
	 */
	public static <A> Cell<A> switchC(final Cell<Cell<A>> bba)
	{
	    return Transaction.apply(new Lambda1<Transaction, Cell<A>>() {
	        public Cell<A> apply(Transaction trans0) {
                Lazy<A> za = bba.sampleLazy().map(new Lambda1<Cell<A>, A>() {
                    @Override
                    public A apply(Cell<A> aCell) {
                        return aCell.sample();
                    }
                });
                final StreamSink<A> out = new StreamSink<A>();
                TransactionHandler<Cell<A>> h = new TransactionHandler<Cell<A>>() {
                    private Listener currentListener;
                    @Override
                    public void run(Transaction trans2, Cell<A> ba) {
                        // Note: If any switch takes place during a transaction, then the
                        // value().listen will always cause a sample to be fetched from the
                        // one we just switched to. The caller will be fetching our output
                        // using value().listen, and value() throws away all firings except
                        // for the last one. Therefore, anything from the old input behaviour
                        // that might have happened during this transaction will be suppressed.
                        if (currentListener != null)
                            currentListener.unlisten();
                        currentListener = ba.value(trans2).listen(out.node, trans2, new TransactionHandler<A>() {
                            public void run(Transaction trans3, A a) {
                                out.send(trans3, a);
                            }
                        }, false);
                    }
        
                    @Override
                    protected void finalize() throws Throwable {
                        if (currentListener != null)
                            currentListener.unlisten();
                    }
                };
                Listener l1 = bba.value().listen_(out.node, h);
                return out.unsafeAddCleanup(l1).holdLazy(za);
            }
        });
	}
	
	/**
	 * Unwrap an event inside a cell to give a time-varying event implementation.
	 */
	public static <A> Stream<A> switchS(final Cell<Stream<A>> bea)
	{
        return Transaction.apply(new Lambda1<Transaction, Stream<A>>() {
        	public Stream<A> apply(final Transaction trans) {
                return switchS(trans, bea);
        	}
        });
    }

	private static <A> Stream<A> switchS(final Transaction trans1, final Cell<Stream<A>> bea)
	{
        final StreamSink<A> out = new StreamSink<A>();
        final TransactionHandler<A> h2 = new TransactionHandler<A>() {
        	public void run(Transaction trans2, A a) {
	            out.send(trans2, a);
	        }
        };
        TransactionHandler<Stream<A>> h1 = new TransactionHandler<Stream<A>>() {
            private Listener currentListener = bea.sampleNoTrans().listen(out.node, trans1, h2, false);

            @Override
            public void run(final Transaction trans2, final Stream<A> ea) {
                trans2.last(new Runnable() {
                	public void run() {
	                    if (currentListener != null)
	                        currentListener.unlisten();
	                    currentListener = ea.listen(out.node, trans2, h2, true);
	                }
                });
            }

            @Override
            protected void finalize() throws Throwable {
                if (currentListener != null)
                    currentListener.unlisten();
            }
        };
        Listener l1 = bea.updates().listen(out.node, trans1, h1, false);
        return out.unsafeAddCleanup(l1);
	}

    /**
     * Transform a cell with a generalized state loop (a mealy machine). The function
     * is passed the input and the old state and returns the new state and output value.
     */
    public final <B,S> Cell<B> collect(final S initState, final Lambda2<A, S, Tuple2<B, S>> f)
    {
        return collect(new Lazy<S>(initState), f);
    }

    /**
     * Transform a cell with a generalized state loop (a mealy machine). The function
     * is passed the input and the old state and returns the new state and output value.
     * Variant that takes a lazy initial state.
     */
    public final <B,S> Cell<B> collect(final Lazy<S> initState, final Lambda2<A, S, Tuple2<B, S>> f)
    {
        return Transaction.<Cell<B>>run(new Lambda0<Cell<B>>() {
            @Override
            public Cell<B> apply() {
                final Stream<A> ea = updates().coalesce(new Lambda2<A,A,A>() {
                    public A apply(A fst, A snd) { return snd; }
                });
                final Lazy<Tuple2<B,S>> zbs = Lazy.<A,S,Tuple2<B,S>>lift(
                        f, sampleLazy(), initState);
                StreamLoop<Tuple2<B,S>> ebs = new StreamLoop<Tuple2<B,S>>();
                Cell<Tuple2<B,S>> bbs = ebs.holdLazy(zbs);
                Cell<S> bs = bbs.map(new Lambda1<Tuple2<B,S>,S>() {
                    public S apply(Tuple2<B,S> x) {
                        return x.b;
                    }
                });
                Stream<Tuple2<B,S>> ebs_out = ea.snapshot(bs, f);
                ebs.loop(ebs_out);
                return bbs.map(new Lambda1<Tuple2<B,S>,B>() {
                    public B apply(Tuple2<B,S> x) {
                        return x.a;
                    }
                });
            }
        });
    }

	@Override
	protected void finalize() throws Throwable {
	    if (cleanup != null)
            cleanup.unlisten();
	}

	/**
	 * Listen for firings of this event. The returned lithium.Listener has an unlisten()
	 * method to cause the listener to be removed. This is the observer pattern.
     */
	public final Listener listen(final Handler<A> action) {
        return Transaction.apply(new Lambda1<Transaction, Listener>() {
        	public Listener apply(final Transaction trans) {
                return value().listen(action);
			}
		});
	}
}
