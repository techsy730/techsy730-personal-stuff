package local.techsy730.function;

public class SimpleParameterPair<T1, T2> implements ParameterPair<T1, T2>
{
    private final T1 first;
    private final T2 second;
    
    public SimpleParameterPair(T1 first, T2 second)
    {
        this.first = first;
        this.second = second;
    }


    @Override
    public boolean hasParameters()
    {
        return true;
    }

    @Override
    public T1 getFirst()
    {
        return first;
    }

    @Override
    public T2 getSecond()
    {
        return second;
    }

}