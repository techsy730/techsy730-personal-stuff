package local.techsy730.function;

public interface Function1<R, T>
    extends
        OneOrMoreParameterFunction<R, T, Void, T, Function0<R>>,
        FunctionBase<R, T>
{
    public R call(T arg);
}
