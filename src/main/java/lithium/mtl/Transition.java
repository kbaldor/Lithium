package lithium.mtl;

import lithium.Transaction;

/**
 *
 * MTL Transition type that uses Transaction identity to handle value finalization.
 *
 * i.e. whenever a transition of TF or FT occurs it will be seen as to FF or TT
 * immediately after the Transaction in which it occurred.
 *
 */
public class Transition {
    public enum Value {
        FF,
        FT,
        TF,
        TT
    }
    Value value;
    Transaction transaction;

    public static Transition FF(){
        return new Transition(Value.FF);
    }
    public static Transition FT(){
        return new Transition(Value.FT);
    }
    public static Transition TF(){
        return new Transition(Value.TF);
    }
    public static Transition TT(){
        return new Transition(Value.TT);
    }

    public Transition(int value, Transaction transaction){
        setIntValue(value);
        this.transaction = transaction;
    }

    public Transition(Value value, Transaction transaction){
        this.value = value;
        this.transaction = transaction;
    }

    public Transition(Value value){
        this.value = value;
        this.transaction = null;
    }

    public Transition(Integer value){
        this.value = Value.values()[value];
        this.transaction = null;
    }

    private Value finalValue(Value value){
        if((value.ordinal()&1)!=0) return Value.TT;
        return Value.FF;
    }

    public Value value(){
        if(Transaction.getCurrentTransaction()!=null) {
            if(this.transaction == null ) {
                this.transaction = Transaction.getCurrentTransaction();
            }
            if(this.transaction != Transaction.getCurrentTransaction()){
                return finalValue(this.value);
            }
        }
        return this.value;
    }
    public int intValue(){return value().ordinal();}
    public void setIntValue(int value){this.value = Value.values()[value];}

    public String toString(){
        return this.value().toString();
    }

}
