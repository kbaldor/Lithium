package lithium.mtl;

import lithium.*;

/**
 * Provides the metric temporal logic operators
 *   NOT, AND, OR, SINCE, and PREVIOUSLY.
 *
 */
public class MTL {

    /**
     *
     * Removes TT and FF transitions when preceded by (TT or FT) or (TF or FF), respectively
     *
     * @param input transition stream
     * @return input Transition stream without redundant transitions
     */
    public static Cell<Transition> filter_redundant(Cell<Transition> input) {
        Cell<Optional<Transition>> last =
                input.value().map(Optional<Transition>::new).hold(new Optional<>(input.sample()));
        input.value().map(Optional<Transition>::new).hold(new Optional<>());
        return input.value().snapshot(last,(in,l)->new Tuple2<>(in,l)).filter(in_l -> {
            if (!in_l.b.isPresent()) return true; // always send the first transition
            int in = in_l.a.intValue();
            int l = in_l.b.get().intValue();
            return ((in != l))||((in & 1) != (in >> 1));
        }).map((in_l) -> in_l.a).hold(input.sample());
    }

    static Lambda1<Transition,Transition> not_op = (p) ->
            new Transition(p.intValue()^3, Transaction.getCurrentTransaction());

    static Lambda2<Transition,Transition,Transition> and_op = (p,  q) ->
        new Transition(p.intValue()&q.intValue(), Transaction.getCurrentTransaction());

    static Lambda2<Transition,Transition,Transition> or_op = (p,  q) ->
        new Transition(p.intValue()|q.intValue(), Transaction.getCurrentTransaction());

    /**
     * This checks for the case when since remains true because p remains true
     */
    static Lambda2<Transition,Transition,Transition> since_hold = (p,s) -> {
        int pp = p.intValue();
        int ppp = pp&2 | pp>>1;
        int ss = s.intValue(); // either 0 or 3
        return new Transition(ss&pp&ppp, Transaction.getCurrentTransaction());
    };

    /**
     * This checks for the case when since remains true because p remains true just after since goes false
     */
    static Lambda2<Transition,Transition,Transition> since_handoff = (p,q) -> {
        int pp = p.intValue();
        int qq = q.intValue();
        int value = qq | ((qq>>1)&pp);
        return new Transition(value, Transaction.getCurrentTransaction());
    };

    public static Cell<Transition> not(Cell<Transition> p){
        return filter_redundant(p.map(not_op));
    }

    public static Cell<Transition> and(Cell<Transition> p, Cell<Transition> q){
        return filter_redundant(Cell.lift(and_op, p, q));
    }

    public static Cell<Transition> or(Cell<Transition> p, Cell<Transition> q){
        return filter_redundant(Cell.lift(or_op, p, q));
    }

    /**
     * The CellLoop requirements of the Java implementation of Sodium
     * makes this look more complicated than it really is.
     *
     *   1. whenever q is true s is true
     *   2. if q is TF and p is FT, s is TT
     *   3. if s is was true in the previous state and p is TT or TF, then s becomes p
     *
     */
    public static Cell<Transition> since(Cell<Transition> p, Cell<Transition> q){
        return Transaction.run(() -> {
            CellLoop<Transition> s = new CellLoop<>();
            s.loop(or(q,
                    or(Cell.lift(since_handoff, p, q),
                            p.updates().snapshot(s, since_hold).hold(q.sample()))));
            return filter_redundant(s);
        });
    }

    public static Cell<Transition> once_cc(Cell<Transition> p, final long delay_ms){
        return Transaction.run(() -> {
            StreamSink<Transition> timeout = new StreamSink<>();
            CellLoop<Optional<TimerQueue.TimerEntry>> handler = new CellLoop<>();
            CellLoop<Transition> o = new CellLoop<>();

            p.updates().snapshot(handler,(t,h)->h).filter(Optional::isPresent).listen(h->TimerQueue.cancel(h.get()));

            Stream<Integer> intValue = p.updates().map(Transition::intValue);

            Stream<Optional<TimerQueue.TimerEntry>> ups = intValue.filter(v->(v&1)==1).map(v->new Optional<>());
            Stream<Optional<TimerQueue.TimerEntry>> downs =
                    intValue.filter(v->(v&1)==0).map(v->(2&v)).
                            map(d -> new Optional<>(TimerQueue.addFutureEvent(delay_ms,
                                    (t) -> timeout.send(new Transition(d)))));

            handler.loop(ups.merge(downs).hold(new Optional<>()));


            o.loop(filter_redundant(p.updates().filter(v -> v.intValue() != 0).snapshot(o, (v, prv) -> new Transition(prv.intValue() | (v.intValue() & 2) | 1)).merge(timeout).hold(p.sample())));
            return o;
        });
    }

}
