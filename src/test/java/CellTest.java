import lithium.Cell;
import lithium.CellSink;
import lithium.Handler;
import lithium.Lambda2;

/**
 * Created by kbaldor on 7/25/15.
 */
public class CellTest {

    public static void main(String[] args){
        CellSink<Integer> a = new CellSink<Integer>(0);
        CellSink<Integer> b = new CellSink<Integer>(0);
        Cell<Integer> c = Cell.lift(new Lambda2<Integer, Integer, Integer>() {
            public Integer apply(Integer a, Integer b) {
                return a+b;
            }
        },a,b);

        c.listen(new Handler<Integer>() {
            public void run(Integer integer) {
                System.out.println("Set c to "+integer);
            }
        });

        a.send(1);
        b.send(2);
    }
}
