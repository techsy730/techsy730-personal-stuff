package local.techsy730.function;

public interface OneOrMoreParameterFunction<R, FirstType, AggregateWithNoFirstType, AggregateType,
    Bound1stFunctionType extends FunctionBase<R, AggregateWithNoFirstType>>
    extends FunctionBase<R, AggregateType>, FunctionTypeWithR<R>
{
    public Bound1stFunctionType bind1(final FirstType argument);
}
