package lithium.mtl;

import lithium.*;

/**
 * Provides the metric temporal logic operators
 *   NOT, AND, OR, and SINCE.
 *
 */
public class MTL {
    static Lambda2<Transition,Transition,Transition> and_op = new Lambda2<Transition,Transition,Transition>() {
        public Transition apply(Transition p, Transition q) {
            Transition out = new Transition(p.intValue()&q.intValue(), Transaction.getCurrentTransaction());
            return out;
        }
    };

    static Lambda2<Transition,Transition,Transition> or_op = new Lambda2<Transition,Transition,Transition>() {
        public Transition apply(Transition p, Transition q) {
            return new Transition(p.intValue()|q.intValue(), Transaction.getCurrentTransaction());
        }
    };

    static Lambda1<Transition,Transition> not_op = new Lambda1<Transition,Transition>() {
        public Transition apply(Transition p) {
            return new Transition(p.intValue()^3, Transaction.getCurrentTransaction());
        }
    };

    /**
     * This checks for the case when since remains true because p remains true
     */
    static Lambda2<Transition,Transition,Transition> since_hold = new Lambda2<Transition,Transition,Transition>() {
        public Transition apply(Transition p, Transition s) {
            int pp = p.intValue();
            int ppp = pp&2 | pp>>1;
            int ss = s.intValue(); // either 0 or 3
            Transition out = new Transition(ss&pp&ppp, Transaction.getCurrentTransaction());
            return out;
        }
    };

    /**
     * This checks for the case when since remains true because p remains true just after since goes false
     */
    static Lambda2<Transition,Transition,Transition> since_handoff = new Lambda2<Transition,Transition,Transition>() {
        public Transition apply(Transition q, Transition p) {
            int pp = p.intValue();
            int qq = q.intValue();
            int value = qq | ((qq>>1)&pp);
            Transition out = new Transition(value, Transaction.getCurrentTransaction());
            return out;
        }
    };

    public static Cell<Transition> not(Cell<Transition> p){
        return p.map(not_op);
    }

    public static Cell<Transition> and(Cell<Transition> p, Cell<Transition> q){
        return Cell.lift(and_op,p,q);
    }

    public static Cell<Transition> or(Cell<Transition> p, Cell<Transition> q){
        return Cell.lift(or_op,p,q);
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
        return Transaction.run(new Lambda0<Cell<Transition>>() {
            @Override
            public Cell<Transition> apply() {
                CellLoop<Transition> s = new CellLoop<Transition>();
                s.loop(or(q,
                        or(Cell.lift(since_handoff, q, p),
                                p.updates().snapshot(s, since_hold).hold(p.sample()))));
                return s;
            }
        });
    }

}
