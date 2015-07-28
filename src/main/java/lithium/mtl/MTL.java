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

    public static Cell<Transition> not(Cell<Transition> p){
        return p.map(not_op);
    }

    public static Cell<Transition> and(Cell<Transition> p, Cell<Transition> q){
        return Cell.lift(and_op,p,q);
    }

    public static Cell<Transition> or(Cell<Transition> p, Cell<Transition> q){
        return Cell.lift(or_op,p,q);
    }

    public static Cell<Transition> since(Cell<Transition> p, Cell<Transition> q){
        return Transaction.run(new Lambda0<Cell<Transition>>() {
            @Override
            public Cell<Transition> apply() {
                CellLoop<Transition> s = new CellLoop<Transition>();
                s.loop(or(q, p.updates().snapshot(s,and_op).hold(p.sample())));
                return s;
            }
        });
    }

}
