/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.grizzly.utils;

import java.util.function.Supplier;

/**
 * The object holder, which might be used for lazy object initialization.
 * 
 *
 */
public abstract class Holder<E> {
    public static <T> Holder<T> staticHolder(final T value) {
        return new Holder<T>() {

            @Override
            public T get() {
                return value;
            }
        };
    }
    
    public static IntHolder staticIntHolder(final int value) {
        return new IntHolder() {

            @Override
            public int getInt() {
                return value;
            }
        };
    }

    public static <T> LazyHolder<T> lazyHolder(final Supplier<T> factory) {
        return new LazyHolder<T>() {

            @Override
            protected T evaluate() {
                return factory.get();
            }
        };
    }
    
    public static LazyIntHolder lazyIntHolder(final Supplier<Integer> factory) {
        return new LazyIntHolder() {

            @Override
            protected int evaluate() {
                return factory.get();
            }
        };
    }
    
    public abstract E get();
    
    protected void set(final E value) {
    }

    @Override
    public String toString() {
        final E obj = get();
        return obj != null ? "{" + obj + "}" : null;
    }

    
    public static abstract class LazyHolder<E> extends Holder<E> {
        private volatile boolean isSet;
        private E value;
        
        @Override
        protected synchronized void set(final E value) {
            this.value = value;
            isSet = true;
        }

        @Override
        public final E get() {
            if (isSet) {
                return value;
            }
            
            synchronized (this) {
                if (!isSet) {
                    value = evaluate();
                    isSet = true;
                }
            }
            
            return value;
        }
        
        protected abstract E evaluate();
    }    
    
    public static abstract class IntHolder extends Holder<Integer> {
        @Override
        public final Integer get() {
            return getInt();
        }

        @Override
        protected final void set(final Integer value) {
            setInt(value);
        }
        
        public abstract int getInt();
        protected void setInt(int value) {
        }
    }
    
    public static abstract class LazyIntHolder extends IntHolder {
        private volatile boolean isSet;
        private int value;
        
        @Override
        protected synchronized void setInt(final int value) {
            this.value = value;
            isSet = true;
        }

        @Override
        public final int getInt() {
            if (isSet) {
                return value;
            }
            
            synchronized (this) {
                if (!isSet) {
                    value = evaluate();
                    isSet = true;
                }
            }
            
            return value;
        }
        
        protected abstract int evaluate();
    }
}
