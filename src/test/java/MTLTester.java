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

    public void testPreviously()
    {
        CellSink<Transition> p = new CellSink<>(Transition.FF());

        Cell<Transition> pr = lithium.mtl.MTL.previously_cc(p,1000);
        ArrayList<Transition.Value> out = new ArrayList<>();
        Listener l = pr.listen(x -> out.add(x.value()));
        try {
            p.send(Transition.TF());
            Thread.sleep(500);
            p.send(Transition.TF());
            Thread.sleep(500);
            p.send(Transition.FT());
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        l.unlisten();
        assertEquals(Arrays.asList(Transition.Value.FF, Transition.Value.TT), out);
    }

    public void testPreviously2()
    {
        CellSink<Transition> p = new CellSink<>(Transition.FF());

        Cell<Transition> pr = lithium.mtl.MTL.previously_cc(p,1000);
        ArrayList<Transition.Value> out = new ArrayList<>();
        Listener l = pr.listen(x -> out.add(x.value()));
        try {
            p.send(Transition.TF());
            Thread.sleep(500);
            p.send(Transition.TF());
            Thread.sleep(500);
            p.send(Transition.TF());
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        l.unlisten();
        assertEquals(Arrays.asList(Transition.Value.FF, Transition.Value.TT, Transition.Value.TF), out);
    }

    public void testDebounce()
    {
        StreamSink<Integer> input = new StreamSink<>();

        Stream<Integer> throttled = Backpressure.debounce(input, 500);
        ArrayList<Integer> out = new ArrayList<>();
        Listener l = throttled.listen(x -> out.add(x));
        try {
            input.send(1);
            Thread.sleep(1000);
            input.send(2);
            Thread.sleep(100);
            input.send(3);
            Thread.sleep(100);
            input.send(4);
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        l.unlisten();
        assertEquals(Arrays.asList(1,4), out);
    }


}

