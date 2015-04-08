package sodium;


public class Lazy<A> {
    public Lazy(Lambda0<A> f) { this.f = f; }
    public Lazy(final A a) { this.f = new Lambda0<A>() {
            public A apply() { return a; }
        };
    }
    private Lambda0<A> f;

    public final A get() {
        return f.apply();
    }

    /**
     * Map the lazy value according to the specified function. 
     */
    public final <B> Lazy<B> map(final Lambda1<A, B> f2) {
        return new Lazy<B>(new Lambda0<B>() {
            public B apply() {
                return f2.apply(get());
            }
        });
    }

	/**
	 * Lift a binary function into lazy values.
	 */
	public static final <A,B,C> Lazy<C> lift(final Lambda2<A,B,C> f, final Lazy<A> a, final Lazy<B> b)
	{
		return new Lazy<C>(new Lambda0<C>() {
            public C apply() {
                return f.apply(a.get(), b.get());
            }
		});
	}

	/**
	 * Lift a ternary function into lazy values.
	 */
	public static final <A,B,C,D> Lazy<D> lift(final Lambda3<A,B,C,D> f, final Lazy<A> a, final Lazy<B> b, final Lazy<C> c)
	{
		return new Lazy<D>(new Lambda0<D>() {
            public D apply() {
                return f.apply(a.get(), b.get(), c.get());
            }
		});
	}

	/**
	 * Lift a quaternary function into lazy values.
	 */
	public static final <A,B,C,D,E> Lazy<E> lift(final Lambda4<A,B,C,D,E> f, final Lazy<A> a, final Lazy<B> b, final Lazy<C> c, final Lazy<D> d)
	{
		return new Lazy<E>(new Lambda0<E>() {
            public E apply() {
                return f.apply(a.get(), b.get(), c.get(), d.get());
            }
		});
	}
}

