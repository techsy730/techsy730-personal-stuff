package local.techsy730.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.PrivilegedAction;

public final class RuntimeTools
{
    
    public static final boolean canGetStack()
    {
        return StackAccessor.canGetStack();
    }
    
    public static final Class<? extends Object>[] getCurrentStack()
    {
        return StackAccessor.getCurrentStack();
    }
    
    public static final Class<? extends Object> getCaller()
    {
        return StackAccessor.getCaller(3);
    }
    
    /**
     * Not really a SecurityManager, just a way to get to {@link SecurityManager#getClassContext()}
     * @author C. Sean Young
     *
     */
    private static final class StackAccessor extends SecurityManager
    {
        static final boolean canGetStack()
        {
            return INSTANCE != null;
        }
        
        @SuppressWarnings("unchecked")
        static final Class<? extends Object>[] getCurrentStack()
        {
            if(INSTANCE == null)
            {
                throw INSTANCE_LOAD_EXCEPTION;
            }
            return INSTANCE.getClassContext();
        }
        
        @SuppressWarnings("unchecked")
        static Class<? extends Object> getCaller(int stackFramesUp)
        {
            if(INSTANCE == null)
            {
                if(SUN_REFLECTION_GET_CALLER_CLASS != null)
                {
                    try
                    {
                        return (Class<? extends Object>)SUN_REFLECTION_GET_CALLER_CLASS.invoke(null, 3);
                    }
                    catch(
                        IllegalAccessException | IllegalArgumentException |
                        InvocationTargetException e)
                    {
                        //TODO Log this exception somehow
                        //Ignore this, just throw the exception gotten when trying to do it the standard way
                    }
                }
                throw INSTANCE_LOAD_EXCEPTION;
            }
            Class<? extends Object>[] stack = getCurrentStack();
            if(stack == null || stack.length < stackFramesUp)
                return null;
            return stack[stackFramesUp];
        }
        
        static SecurityException INSTANCE_LOAD_EXCEPTION = null;
        
        private static final StackAccessor INSTANCE = 
            java.security.AccessController.doPrivileged(
                new PrivilegedAction<StackAccessor>()
                {
                    @Override
                    public StackAccessor run()
                    {
                        try
                        {
                            return new StackAccessor();
                        }
                        catch(SecurityException err)
                        {
                            //TODO log this exception
                            //Can't get the "backdoor" for stack access.
                            INSTANCE_LOAD_EXCEPTION = err;
                            return null;
                        }
                    }                    
                });
        
        private static final Method SUN_REFLECTION_GET_CALLER_CLASS = 
            java.security.AccessController.doPrivileged(
                new PrivilegedAction<Method>()
                {
                    @Override
                    public Method run()
                    {
                        try
                        {
                            return Class.forName("sun.reflect.Reflection").
                                getMethod("getCallerClass", int.class);
                        }
                        catch(ReflectiveOperationException | SecurityException err)
                        {
                            //TODO log this exception
                            //Can't get the "backdoor" for stack access.
                            return null;
                        }
                    }                    
                });
        
        public StackAccessor(){}
    }
}
