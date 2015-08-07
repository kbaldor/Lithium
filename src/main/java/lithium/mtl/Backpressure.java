package lithium.mtl;

import lithium.Cell;
import lithium.Optional;
import lithium.Stream;

/**
 * Created by kbaldor on 8/5/15.
 */
public class Backpressure {
    public static <Type> Stream<Type> debounce(Stream<Type> input, long period_ms){
        Cell<Optional<Type>> lastValue = input.map(i->new Optional<>(i)).hold(new Optional<>());
        Cell<Transition> arrivals = input.map(i->Transition.TF()).hold(Transition.FF());
        Cell<Transition> throttling = MTL.previously_cc(arrivals,period_ms);
        return throttling.updates().filter(v->v.intValue()==2).snapshot(lastValue,(e,v)->v.get());
    }
}
