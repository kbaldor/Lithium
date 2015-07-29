import junit.framework.TestCase;
import lithium.*;
import lithium.mtl.Transition;
import lithium.mtl.MTL.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



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
        Listener l = n.listen(x -> { out.add(x.value());});
        p.send(Transition.TF());
        p.send(Transition.FF());
        l.unlisten();
        assertEquals(Arrays.asList(Transition.Value.TT, Transition.Value.FT, Transition.Value.TT), out);
    }


    public void testAnd()
    {
        CellSink<Transition> p = new CellSink<>(Transition.FF());
        CellSink<Transition> q = new CellSink<>(Transition.TT());
        CellSink<Transition> r = new CellSink<>(Transition.TT());

        Cell<Transition> a = lithium.mtl.MTL.and(lithium.mtl.MTL.and(p, q),r);
        ArrayList<Transition.Value> out = new ArrayList<>();
        Listener l = a.updates().listen(x -> { out.add(x.value());});
        Transaction.run(new Handler<Transaction>() {
            @Override
            public void run(Transaction transaction) {
                p.send(Transition.FT());
                q.send(Transition.TF());
            }
        });
        Transaction.run(new Handler<Transaction>() {
            @Override
            public void run(Transaction transaction) {
                p.send(Transition.FT());
                q.send(Transition.TT());
            }
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
        Listener l = a.updates().listen(x -> { out.add(x.value());});
        Transaction.run(new Handler<Transaction>() {
            @Override
            public void run(Transaction transaction) {
                p.send(Transition.TF());
                q.send(Transition.TF());
            }
        });
        Transaction.run(new Handler<Transaction>() {
            @Override
            public void run(Transaction transaction) {
                p.send(Transition.FT());
                q.send(Transition.TT());
            }
        });
        r.send(Transition.TF());
        l.unlisten();
        assertEquals(Arrays.asList(Transition.Value.TF, Transition.Value.TT, Transition.Value.TT), out);
    }

    public void testSince()
    {
        CellSink<Transition> p = new CellSink<>(Transition.FF());
        CellSink<Transition> q = new CellSink<>(Transition.TT());

        Cell<Transition> s = lithium.mtl.MTL.since(p,q);

        ArrayList<Transition.Value> out = new ArrayList<>();
//        Listener l = s.updates().listen(x -> { out.add(x.value());});
        Listener l = s.listen(x -> {
            out.add(x.value());
        });
        Transaction.run(new Handler<Transaction>() {
            @Override
            public void run(Transaction transaction) {
                p.send(Transition.FT());
                q.send(Transition.TF());
            }
        });
        p.send(Transition.FT());
        p.send(Transition.TF());
        q.send(Transition.TF());
        q.send(Transition.FT());
        p.send(Transition.TT());
        q.send(Transition.FF());
        l.unlisten();
        assertEquals(Arrays.asList(
                Transition.Value.TT, // initial value
                Transition.Value.TT,
                Transition.Value.FF,
                Transition.Value.FF,
                Transition.Value.TF,
                Transition.Value.FT,
                Transition.Value.TT,
                Transition.Value.TT), out);
    }


}

