import junit.framework.TestCase;
import lithium.*;
import lithium.mtl.Backpressure;
import lithium.mtl.Transition;

import java.util.ArrayList;
import java.util.Arrays;



public class MTLTester extends TestCase {
	@Override
	protected void tearDown() throws Exception {
		System.gc();
		Thread.sleep(100);
	}

    public void testNot()
    {
        CellSink<Transition> p = new CellSink<>(Transition.FF());

        Cell<Transition> n = lithium.mtl.MTL.not(p);
        ArrayList<Transition.Value> out = new ArrayList<>();
        Listener l = n.listen(x -> out.add(x.value()));
        p.send(Transition.TF());
        p.send(Transition.TT());
        l.unlisten();
        assertEquals(Arrays.asList(Transition.Value.TT, Transition.Value.FT, Transition.Value.FF), out);
    }

    public void testAnd()
        {
            CellSink<Transition> p = new CellSink<>(Transition.FF());
            CellSink<Transition> q = new CellSink<>(Transition.TT());
            CellSink<Transition> r = new CellSink<>(Transition.TT());

            Cell<Transition> a = lithium.mtl.MTL.and(lithium.mtl.MTL.and(p, q),r);
            ArrayList<Transition.Value> out = new ArrayList<>();
            Listener l = a.listen(x -> out.add(x.value()));
            Transaction.run((t) -> {
                    p.send(Transition.FT());
                    q.send(Transition.TF());
            });
            Transaction.run((t) -> {
                    p.send(Transition.FT());
                    q.send(Transition.TT());
            });
            r.send(Transition.TF());
            l.unlisten();
            assertEquals(Arrays.asList(Transition.Value.FF, Transition.Value.FT, Transition.Value.TF), out);
        }

        public void testOr()
        {
            CellSink<Transition> p = new CellSink<>(Transition.FF());
            CellSink<Transition> q = new CellSink<>(Transition.TT());
            CellSink<Transition> r = new CellSink<>(Transition.FF());

            Cell<Transition> a = lithium.mtl.MTL.or(lithium.mtl.MTL.or(p, q), r);
            ArrayList<Transition.Value> out = new ArrayList<>();
            Listener l = a.updates().listen(x -> out.add(x.value()));
            Transaction.run(t ->  {
                p.send(Transition.TF());
                q.send(Transition.TF());

            });
            Transaction.run(t -> {
                p.send(Transition.FT());
                q.send(Transition.TT());

            });
            r.send(Transition.TF());
            l.unlisten();
            assertEquals(Arrays.asList(Transition.Value.TF, Transition.Value.TT), out);
        }

        public void testSince()
        {
            CellSink<Transition> p = new CellSink<>(Transition.TT());
            CellSink<Transition> q = new CellSink<>(Transition.FF());

            Cell<Transition> s = lithium.mtl.MTL.since(p,q);

            ArrayList<Transition.Value> out = new ArrayList<>();
            Listener l = s.listen(x -> out.add(x.value()));
            Transaction.run(t -> {
                p.send(Transition.FT());
                q.send(Transition.TF());
            });
            p.send(Transition.FT());
            p.send(Transition.TF());
            q.send(Transition.TF());
            q.send(Transition.FT());
            p.send(Transition.TT());
            q.send(Transition.FF());
            l.unlisten();
            assertEquals(Arrays.asList(
                    Transition.Value.FF, // initial value
                    Transition.Value.TT,
                    Transition.Value.FF,
                    Transition.Value.TF,
                    Transition.Value.FT), out);
        }

    public void testOnce_cc()
    {
        CellSink<Transition> p = new CellSink<>(Transition.FF());

        Cell<Transition> o = lithium.mtl.MTL.once_cc(p, 100);
        ArrayList<Transition.Value> out = new ArrayList<>();
        Listener l = o.listen(x -> out.add(x.value()));
        try {
            p.send(Transition.TF());
            Thread.sleep(50);
            p.send(Transition.TF());
            Thread.sleep(50);
            p.send(Transition.FT());
            Thread.sleep(150);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        l.unlisten();
        assertEquals(Arrays.asList(Transition.Value.FF, Transition.Value.TT), out);
    }

    public void testOnce_cc2()
    {
        CellSink<Transition> p = new CellSink<>(Transition.FF());

        Cell<Transition> o = lithium.mtl.MTL.once_cc(p, 100);
        ArrayList<Transition.Value> out = new ArrayList<>();
        Listener l = o.listen(x -> out.add(x.value()));
        try {
            p.send(Transition.TF());
            Thread.sleep(50);
            p.send(Transition.TF());
            Thread.sleep(50);
            p.send(Transition.TF());
            Thread.sleep(150);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        l.unlisten();
        assertEquals(Arrays.asList(Transition.Value.FF, Transition.Value.TT, Transition.Value.TF), out);
    }

    public void testOnce_oc2()
    {
        CellSink<Transition> p = new CellSink<>(Transition.FF());

        Cell<Transition> o = lithium.mtl.MTL.once_oc(p, 100);
        ArrayList<Transition.Value> out = new ArrayList<>();
        Listener l = o.listen(x -> out.add(x.value()));
        try {
            p.send(Transition.TF());
            Thread.sleep(50);
            p.send(Transition.TF());
            Thread.sleep(50);
            p.send(Transition.TF());
            Thread.sleep(150);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        l.unlisten();
        assertEquals(Arrays.asList(Transition.Value.FF, Transition.Value.FT, Transition.Value.TF), out);
    }


    public void testOnce_co2()
    {
        CellSink<Transition> p = new CellSink<>(Transition.FF());

        Cell<Transition> o = lithium.mtl.MTL.once_co(p, 100);
        ArrayList<Transition.Value> out = new ArrayList<>();
        Listener l = o.listen(x -> out.add(x.value()));
        try {
            p.send(Transition.TF());
            Thread.sleep(50);
            p.send(Transition.TF());
            Thread.sleep(50);
            p.send(Transition.TF());
            Thread.sleep(150);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        l.unlisten();
        assertEquals(Arrays.asList(Transition.Value.FF, Transition.Value.TT, Transition.Value.FF), out);
    }

    public void testOnce_oo2()
    {
        CellSink<Transition> p = new CellSink<>(Transition.FF());

        Cell<Transition> o = lithium.mtl.MTL.once_oo(p, 100);
        ArrayList<Transition.Value> out = new ArrayList<>();
        Listener l = o.listen(x -> out.add(x.value()));
        try {
            p.send(Transition.TF());
            Thread.sleep(50);
            p.send(Transition.TF());
            Thread.sleep(50);
            p.send(Transition.TF());
            Thread.sleep(150);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        l.unlisten();
        assertEquals(Arrays.asList(Transition.Value.FF, Transition.Value.FT, Transition.Value.FF), out);
    }


    public void testDebounce()
    {
        StreamSink<Integer> input = new StreamSink<>();

        Stream<Integer> debounced = Backpressure.debounce(input, 50);
        ArrayList<Integer> out = new ArrayList<>();
        Listener l = debounced.listen(x -> out.add(x));
        try {
            input.send(1);
            Thread.sleep(100);
            input.send(2);
            Thread.sleep(10);
            input.send(3);
            Thread.sleep(10);
            input.send(4);
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        l.unlisten();
        assertEquals(Arrays.asList(1,4), out);
    }

    public void testThrottleFirst()
    {
        StreamSink<Integer> input = new StreamSink<>();

        Stream<Integer> throttled = Backpressure.throttleFirst(input, 50);
        ArrayList<Integer> out = new ArrayList<>();
        Listener l = throttled.listen(x -> out.add(x));
        try {
            input.send(1);
            Thread.sleep(100); // should emit 1 during this sleep
            input.send(2);
            Thread.sleep(15);
            input.send(3);
            Thread.sleep(10);
            input.send(4);
            Thread.sleep(10);
            input.send(5);
            Thread.sleep(10);
            input.send(6);
            Thread.sleep(10); // should emit 2 during this sleep
            input.send(7);
            Thread.sleep(10);
            input.send(8);
            Thread.sleep(10);
            input.send(9);
            Thread.sleep(100); // should emit 7 during this sleep
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        l.unlisten();
        assertEquals(Arrays.asList(1,2,7), out);
    }

    public void testThrottleLast()
    {
        StreamSink<Integer> input = new StreamSink<>();

        Stream<Integer> throttled = Backpressure.throttleLast(input, 50);
        ArrayList<Integer> out = new ArrayList<>();
        Listener l = throttled.listen(x -> out.add(x));
        try {
            input.send(1);
            Thread.sleep(100); // should emit 1 during this sleep
            input.send(2);
            Thread.sleep(15);
            input.send(3);
            Thread.sleep(10);
            input.send(4);
            Thread.sleep(10);
            input.send(5);
            Thread.sleep(10);
            input.send(6);
            Thread.sleep(10); // shoule emit 6 during this sleep
            input.send(7);
            Thread.sleep(10);
            input.send(8);
            Thread.sleep(10);
            input.send(9);
            Thread.sleep(100); // should emit 7 during this sleep
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        l.unlisten();
        assertEquals(Arrays.asList(1,6,9), out);
    }

}

