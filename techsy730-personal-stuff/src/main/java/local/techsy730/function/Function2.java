package local.techsy730.function;

public interface Function2<R, T1, T2>
    extends
        TwoOrMoreParameterFunction<R, T1, T2, T2, T1, Void, ParameterPair<T1, T2>,
            Function0<R>,
            Function1<R, T2>,
            Function1<R, T1>>,
        FunctionBase<R, ParameterPair<T1, T2>>
{
    public R call(T1 arg1, T2 arg2);
}
