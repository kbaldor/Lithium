package lithium.mtl;

import lithium.*;

/**
 * Created by kbaldor on 8/5/15.
 */
public class Backpressure {
    public static <Type> Stream<Type> debounce(Stream<Type> input, long period_ms){
        Cell<Optional<Type>> lastValue = input.map(i->new Optional<>(i)).hold(new Optional<>());
        Cell<Transition> arrivals = input.map(i->Transition.TF()).hold(Transition.FF());
        Cell<Transition> debouncing = MTL.once_cc(arrivals, period_ms);
        return debouncing.updates().filter(v->v.intValue()==2).snapshot(lastValue,(e,v)->v.get());
    }

    public static <Type> Stream<Type> throttleFirst(Stream<Type> input, long period_ms){
        return Transaction.run(() -> {
            CellLoop<Transition> throttling = new CellLoop<>();
            Cell<Optional<Type>> firstValue = input.snapshot(throttling,(i,t)->new Tuple2<Type,Transition>(i,t))
                    .filter(v -> v.b.intValue() == 0)
                    .map(i -> new Optional<>(i.a))
                    .hold(new Optional<>());
            Cell<Transition> arrivals = input.snapshot(throttling, (i, t) -> t)
                    .filter(t -> t.intValue() == 0)
                    .map(i -> Transition.TF())
                    .hold(Transition.FF());

            throttling.loop(MTL.once_cc(arrivals, period_ms));
            return throttling.updates().filter(v -> v.intValue() == 2).snapshot(firstValue, (e, v) -> v.get());
        });
    }

    public static <Type> Stream<Type> throttleLast(Stream<Type> input, long period_ms){
        return Transaction.run(() -> {
            Cell<Optional<Type>> lastValue = input.map(i -> new Optional<>(i)).hold(new Optional<>());
            CellLoop<Transition> throttling = new CellLoop<>();
            Cell<Transition> arrivals = input.snapshot(throttling, (i, t) -> t)
                    .filter(t -> t.intValue() == 0)
                    .map(i -> Transition.TF())
                    .hold(Transition.FF());

            throttling.loop(MTL.once_cc(arrivals, period_ms));
            return throttling.updates().filter(v -> v.intValue() == 2).snapshot(lastValue, (e, v) -> v.get());
        });
    }
}
