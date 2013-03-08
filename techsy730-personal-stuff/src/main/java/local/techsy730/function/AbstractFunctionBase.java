package local.techsy730.function;

/**
 * A skeletal implementation for function types.
 * This class defines {@link #callRoot(Object) callRoot(T)} on top of its {@link #callUnsafe(Object...) callUnsafe(Object...)}
 * function, due to {@link #callUnsafe(Object...) callUnsafe(Object...)} being more intuitive to implement.
 * @author C. Sean Young
 *
 * @param <R> {@inheritDoc}
 * @param <T> {@inheritDoc}
 */
public abstract class AbstractFunctionBase<R, T> implements FunctionBase<R, T>
{
    @Override
    public R callRoot(T argument)
    {
        return callUnsafe(unpackArguments(argument, getArgumentCount()));
    }
    
    final void checkArgumentCount(int givenNumArguments)
    {
        if(givenNumArguments != getArgumentCount())
        {
            throw new IllegalArgumentException("Invalid number of arguments. Recieved: " + givenNumArguments + " Expected: " + getArgumentCount());
        }
    }
    
    final void checkArgumentCount(Object[] arguments)
    {
        checkArgumentCount(arguments == null ? 0 : arguments.length);
    }

    @Override
    public abstract R callUnsafe(Object... arguments);
    
    private static final Object[] EMPTY_OBJECT_ARRAY = local.techsy730.util.ArrayUtils.EMPTY_OBJECT_ARRAY;
    
    private static Object[] unpackArguments(Object arg, int argGuess)
    {
        if(arg == null)
            return EMPTY_OBJECT_ARRAY;
        if(!(arg instanceof ParameterPair))
            return new Object[]{arg};
        java.util.List<Object> buildUp = new java.util.ArrayList<>(argGuess);
        ParameterPair<?, ?> pair = (ParameterPair<?, ?>)arg;
        Object firstArg;
        Object secondArg;
        boolean isSecondArgPair;
        do
        {
            //Add the first argument, and then "recurse" if the second argument is itself another pair
            firstArg = pair.getFirst();
            secondArg = pair.getSecond();
            isSecondArgPair = secondArg instanceof ParameterPair;
            buildUp.add(firstArg);
            if(isSecondArgPair)
            {
                pair = (ParameterPair<?, ?>)secondArg;
            }
        } while(isSecondArgPair);
        buildUp.add(secondArg);
        return buildUp.toArray();
    }

}
