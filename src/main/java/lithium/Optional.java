package lithium;

/**
 * Created by kbaldor on 4/7/15.
 */
public class Optional <T> {
    private T myValue;

    public Optional(){
        myValue = null;
    }
    public Optional(T value){
        myValue = value;
    }

    public boolean isPresent(){
        return myValue != null;
    }

    public T get(){
        return myValue;
    }

    static public <T> Optional<T> empty(){
        return new Optional<T>();
    }

    static public <T> Optional<T> of(T value){
        return new Optional<T>(value);
    }
}
