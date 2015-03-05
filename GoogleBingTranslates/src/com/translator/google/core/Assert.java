/**
 * @author <a href=mailto:volkodavav@gmail.com>volkodavav</a>
 */
package com.translator.google.core;

import java.util.Collection;

public abstract class Assert
{
    public static void containText(String string)
        throws IllegalArgumentException
    {
        containText(string,
                    "[ *** Assertion failed *** ] String must not be null or empty");
    }

    public static void containText(String string, String message)
        throws IllegalArgumentException
    {
        if (string == null || string.trim().isEmpty())
        {
            throw new IllegalArgumentException(message);
        }
    }

    public static void notEmpty(Collection<?> collection)
        throws IllegalArgumentException
    {
        notEmpty(collection,
                 "[ *** Assertion failed *** ] Collection must not be null or empty");
    }

    public static void notEmpty(Collection<?> collection, String message)
        throws IllegalArgumentException
    {
        if (collection == null || collection.isEmpty())
        {
            throw new IllegalArgumentException(message);
        }
    }

    public static void notEmpty(String string) throws IllegalArgumentException
    {
        notEmpty(string,
                 "[ *** Assertion failed *** ] String must not be null or empty");
    }

    public static void notEmpty(String string, String message)
        throws IllegalArgumentException
    {
        if (string == null || string.isEmpty())
        {
            throw new IllegalArgumentException(message);
        }
    }

    public static void notNull(Object object) throws IllegalArgumentException
    {
        notNull(object,
                "[ *** Assertion failed *** ] Argument must not be null");
    }

    public static void notNull(Object object, String message)
        throws IllegalArgumentException
    {
        if (object == null)
        {
            throw new IllegalArgumentException(message);
        }
    }

    public static void isTrue(boolean expression)
        throws IllegalArgumentException
    {
        isTrue(expression,
               "[ *** Assertion failed *** ] Expression must be true");
    }

    public static void isTrue(boolean expression, String message)
        throws IllegalArgumentException
    {
        if (!expression)
        {
            throw new IllegalArgumentException(message);
        }
    }

    public static void validState(boolean state) throws IllegalStateException
    {
        validState(state, "[ *** Assertion failed *** ] State must be true");
    }

    public static void validState(boolean state, String message)
        throws IllegalStateException
    {
        if (!state)
        {
            throw new IllegalStateException(message);
        }
    }

    private Assert()
    {
    }
}
