/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http.util;

import org.glassfish.grizzly.Buffer;
import java.nio.charset.Charset;

/**
 * {@link Buffer} chunk representation.
 * Helps HTTP module to avoid redundant String creation.
 * 
 * @author Alexey Stashok
 */
public class BufferChunk {
    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    
    public static BufferChunk newInstance() {
        return new BufferChunk();
    }
    
    Buffer buffer;

    int start;
    int end;

    String cachedString;
    Charset cachedStringCharset;

    final CharChunk charChunk;
    
    String stringValue;

    protected BufferChunk() {
        this(new CharChunk());
    }

    BufferChunk(final CharChunk charChunk) {
        this.charChunk = charChunk;
    }

    public BufferChunk toImmutable() {
        return new Immutable(this);
    }

    public void set(BufferChunk value) {
        reset();

        if (value.hasBuffer()) {
            setBuffer(value.buffer, value.start, value.end);
            cachedString = value.cachedString;
            cachedStringCharset = value.cachedStringCharset;
        } else if (value.hasString()) {
            stringValue = value.stringValue;
        }

        onContentChanged();
    }

    public Buffer getBuffer() {
        return buffer;
    }

    public void setBuffer(Buffer buffer) {
        setBuffer(buffer, buffer.position(), buffer.limit());
    }
    
    public void setBuffer(Buffer buffer, int start, int end) {
        this.buffer = buffer;
        this.start = start;
        this.end = end;
        
        resetString();
        resetStringCache();
        onContentChanged();
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
        resetStringCache();
        onContentChanged();
    }
    
    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
        resetStringCache();
        onContentChanged();
    }

    public void setString(String string) {
        stringValue = string;
        resetBuffer();
        onContentChanged();
    }

    @Override
    public String toString() {
        return toString(null);
    }

    public String toString(int start, int end) {
        return toString(null, start, end);
    }

    public String toString(Charset charset) {
        return toString(charset, start, end);
    }
    
    public String toString(Charset charset, int start, int end) {
        if (isNull()) return null;
        
        if (hasString()) return stringValue;

        if (charset == null) charset = UTF8_CHARSET;

        if (cachedString != null && charset.equals(cachedStringCharset)) {
            return cachedString;
        }

        cachedString = buffer.toStringContent(charset, start, end);
        cachedStringCharset = charset;
        
        return cachedString;
    }

    protected void onContentChanged() {
    }

    /**
     * Returns the <tt>BufferChunk</tt> length.
     *
     * @return the <tt>BufferChunk</tt> length.
     */
    public int size() {
        if (hasBuffer()) {
            return end - start;
        } else if (hasString()) {
            return stringValue.length();
        }

        return 0;
    }
    

    /**
     * Returns the starting index of the specified byte sequence within this
     * <code>Buffer</code>.
     *
     * @param b byte sequence to search for.
     *
     * @return the starting index of the specified byte sequence within this
     *  <code>Buffer</code>
     */
    public int findBytesAscii(byte[] b) {

        final byte first = b[0];
        final int from = getStart();
        final int to = getEnd();

        // Look for first char
        int srcEnd = b.length;

        for (int i = from; i <= to - srcEnd; i++) {
            if (Ascii.toLower(buffer.get(i)) != first) continue;
            // found first char, now look for a match
            int myPos = i + 1;
            for (int srcPos = 1; srcPos < srcEnd;) {
                if (Ascii.toLower(buffer.get(myPos++)) != b[srcPos++]) {
                    break;
                }
                if (srcPos == srcEnd) {
                    return i - from; // found it
                }
            }
        }
        return -1;
    }


    /**
     * Returns true if the message bytes starts with the specified string.
     * @param c the character
     * @param fromIndex The start position
     */
    public int indexOf(char c, int fromIndex) {
        if (hasBuffer()) {
            int ret = indexOf(buffer, start + fromIndex, end, c);
            return (ret >= start) ? ret - start : -1;
        } else if (hasString()) {
            return stringValue.indexOf(c, fromIndex);
        }

        return -1;
    }

    /**
     * Returns true if the message bytes starts with the specified string.
     * @param s the string
     * @param fromIndex The start position
     */
    public int indexOf(String s, int fromIndex) {
        if (hasBuffer()) {
            int ret = indexOf(buffer, start + fromIndex, end, s);
            return (ret >= start) ? ret - start : -1;
        } else if (hasString()) {
            return stringValue.indexOf(s, fromIndex);
        }

        return -1;
    }

    private static int indexOf(Buffer buffer, int off, int end, char qq) {
        // Works only for UTF
        while (off < end) {
            byte b = buffer.get(off);
            if (b == qq) {
                return off;
            }
            off++;
        }
        return -1;
    }

    private static int indexOf(Buffer buffer, int off, final int end, final CharSequence s) {
        // Works only for UTF
        final int strLen = s.length();
        if (strLen == 0) {
            return off;
        }
        
        if (strLen > (end - off)) return -1;
        
        int strOffs = 0;
        final int lastOffs = end - strLen;

        while (off <= lastOffs + strOffs) {
            final byte b = buffer.get(off);
            if (b == s.charAt(strOffs)) {
                strOffs++;
                if (strOffs == strLen) {
                    return off - strLen + 1;
                }
            } else {
                strOffs = 0;
            }

            off++;
        }
        return -1;
    }
    
    /**
     * Compares the message bytes to the specified String object.
     * @param s the String to compare
     * @return true if the comparison succeeded, false otherwise
     */
    public boolean equals(String s) {
        if (!hasString()) {
            if ((end - start) != s.length()) {
                return false;
            }

            for(int i = start; i < end; i++) {
                if (buffer.get(i) != s.charAt(i - start)) {
                    return false;
                }
            }

            return true;
        } else {
            return stringValue.equals(s);
        }
    }

    /**
     * Compares the message bytes to the specified String object.
     * @param s the String to compare
     * @return true if the comparison succeeded, false otherwise
     */
    public boolean equalsIgnoreCase(String s) {
        if (!hasString()) {
            if ((end - start) != s.length()) {
                return false;
            }
            
            for(int i = start; i < end; i++) {
                if (Ascii.toLower(buffer.get(i)) != Ascii.toLower(s.charAt(i - start))) {
                    return false;
                }
            }
            
            return true;
        } else {
            return stringValue.equalsIgnoreCase(s);
        }
    }

    /**
     * @return -1, 0 or +1 if inferior, equal, or superior to the String.
     */
    public int compareIgnoreCase(int start, int end, String compareTo) {
        int result = 0;

        int len = compareTo.length();
        if ((end - start) < len) {
            len = end - start;
        }
        for (int i = 0; (i < len) && (result == 0); i++) {
            if (Ascii.toLower(buffer.get(i + start)) > Ascii.toLower(compareTo.charAt(i))) {
                result = 1;
            } else if (Ascii.toLower(buffer.get(i + start)) < Ascii.toLower(compareTo.charAt(i))) {
                result = -1;
            }
        }
        if (result == 0) {
            if (compareTo.length() > (end - start)) {
                result = -1;
            } else if (compareTo.length() < (end - start)) {
                result = 1;
            }
        }
        return result;
    }


    /**
     * @return -1, 0 or +1 if inferior, equal, or superior to the String.
     */
    public int compare(int start, int end, String compareTo) {
        int result = 0;
        int len = compareTo.length();
        if ((end - start) < len) {
            len = end - start;
        }
        for (int i = 0; (i < len) && (result == 0); i++) {
            if (buffer.get(i + start) > compareTo.charAt(i)) {
                result = 1;
            } else if (buffer.get(i + start) < compareTo.charAt(i)) {
                result = -1;
            }
        }
        if (result == 0) {
            if (compareTo.length() > (end - start)) {
                result = -1;
            } else if (compareTo.length() < (end - start)) {
                result = 1;
            }
        }
        return result;
    }



    /**
     * Returns <code>true</code> if the <code>BufferChunk</code> starts with
     * the specified string.
     *  
     * @param s the string
     * @param pos The start position
     *
     * @return <code>true</code> if the </code>BufferChunk</code> starts with
     *  the specified string.
     */
    public boolean startsWithIgnoreCase(String s, int pos) {
        if (!hasString()) {
            int len = s.length();
            if (len > end - start - pos) {
                return false;
            }
            
            int off = start + pos;
            for (int i = 0; i < len; i++) {
                if (Ascii.toLower(buffer.get(off++)) != Ascii.toLower(s.charAt(i))) {
                    return false;
                }
            }
            
            return true;
        } else {
            if (stringValue.length() < pos + s.length()) {
                return false;
            }

            for (int i = 0; i < s.length(); i++) {
                if (Ascii.toLower(s.charAt(i))
                        != Ascii.toLower(stringValue.charAt(pos + i))) {
                    return false;
                }
            }
            return true;
        }
    }


    /**
     * Returns <code>true</code> if the <code>BufferChunk</code> starts with
     * the specified string.
     * @param s the string
     * @param pos The start position
     *
     * @return <code>true</code> if the <code>BufferChunk</code> starts with
     *  the specified string.
     */
    public boolean startsWith(String s, int pos) {
        if (!hasString()) {
            int len = s.length();
            if (len > end - start - pos) {
                return false;
            }

            int off = start + pos;
            for (int i = 0; i < len; i++) {
                if (buffer.get(off++) != s.charAt(i)) {
                    return false;
                }
            }

            return true;
        } else {
            if (stringValue.length() < pos + s.length()) {
                return false;
            }

            for (int i = 0; i < s.length(); i++) {
                if (s.charAt(i) != stringValue.charAt(pos + i)) {
                    return false;
                }
            }
            return true;
        }
    }
    
    public final boolean hasBuffer() {
        return buffer != null;
    }

    public final boolean hasString() {
        return stringValue != null;
    }
    
    public final boolean isNull() {
        return !hasBuffer() && !hasString();
    }

    protected void resetBuffer() {
        start = -1;
        end = -1;
        buffer = null;
    }

    protected void resetString() {
        stringValue = null;
        charChunk.recycle();
    }
    
    protected void resetStringCache() {
        cachedString = null;
        cachedStringCharset = null;
    }
    
    protected void reset() {
        start = -1;
        end = -1;
        buffer = null;
        cachedString = null;
        cachedStringCharset = null;
        stringValue = null;
        charChunk.recycle();
    }

    public void recycle() {
        reset();
    }

    final static class Immutable extends BufferChunk {
        public Immutable(BufferChunk original) {
            super(original.charChunk);
            this.start = original.start;
            this.end = original.end;
            this.buffer = original.buffer;
            this.cachedString = original.cachedString;
            this.cachedStringCharset = original.cachedStringCharset;
            this.stringValue = original.stringValue;
        }

        @Override
        public BufferChunk toImmutable() {
            return this;
        }

        @Override
        public void set(BufferChunk value) {
            return;
        }

        @Override
        public void setBuffer(Buffer buffer, int start, int end) {
            return;
        }

        @Override
        public void setStart(int start) {
            return;
        }

        @Override
        public void setEnd(int end) {
            return;
        }

        @Override
        public void setString(String string) {
            return;
        }

        @Override
        protected final void resetBuffer() {
            return;
        }

        @Override
        protected final void resetString() {
            return;
        }

        @Override
        protected final void resetStringCache() {
            return;
        }

        @Override
        protected void reset() {
            return;
        }

        @Override
        public void recycle() {
                return;
        }
    }
}
