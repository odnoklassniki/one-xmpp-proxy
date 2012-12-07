/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package one.xmpp.xml;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.util.Arrays;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import one.xmpp.server.errors.stream.BadFormatError;
import one.xmpp.utils.CharSequenceUtils;
import one.xmpp.xml.buffer.IoBuffer;

/**
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class XmlTokenizer {

    private static final Log log = LogFactory.getLog(XmlTokenizer.class);

    private static final char NO_CHAR = (char) -1;

    private static boolean isControlChar(char c) {
        return c == '<' || c == '>' || c == '!' || c == '/' || c == '?' || c == '=';
    }

    private final IoBuffer buffer = IoBuffer.allocate(16).setAutoExpand(true);

    private TokenListener listener;

    private State state = State.START;

    public XmlTokenizer(TokenListener listeners) {
        this.listener = listeners;
    }

    public void close() {
        state = State.CLOSED;
        buffer.clear();
    }

    private void emit(char token) throws BadFormatError {
        listener.token(token, null);
    }

    private void emitBuffer() throws BadFormatError {
        try {
            buffer.flip();
            final CharSequence characters = CharSequenceUtils.decodeFromUtf8(buffer.buf());
            listener.token(NO_CHAR, characters);
            buffer.clear();
        } catch (CharacterCodingException e) {
            if (log.isDebugEnabled()) {
                try {
                    log.debug("Buffer content: " + buffer.buf() + ": " + Arrays.toString(buffer.buf().array()));
                } catch (Throwable exc) {
                    log.warn(exc, exc);
                }
            }
            throw new BadFormatError("Unable to decode bytes as UTF-8: " + e, e);
        }
    }

    /**
     * @param byteBuffer
     * @param charsetDecoder
     * @return the new particle or NULL, if the buffer was exhausted before the particle was
     *         completed
     * @throws Exception
     */
    public void parse(ByteBuffer byteBuffer) throws BadFormatError {
        while (byteBuffer.hasRemaining() && state != State.CLOSED) {
            final byte b = byteBuffer.get();
            final char c = (char) b;

            try {

                if (state == State.START) {
                    if (c == '<') {
                        emit(c);
                        state = State.IN_TAG;
                    } else {
                        state = State.IN_TEXT;
                        buffer.put((byte) c);
                    }
                } else if (state == State.IN_TEXT) {
                    if (c == '<') {
                        emitBuffer();
                        emit(c);
                        state = State.IN_TAG;
                    } else {
                        buffer.put(b);
                    }
                } else if (state == State.IN_TAG) {
                    if (c == '>') {
                        emit(c);
                        state = State.START;
                    } else if (c == '"') {
                        emit(c);
                        state = State.IN_DOUBLE_ATTRIBUTE_VALUE;
                    } else if (c == '\'') {
                        emit(c);
                        state = State.IN_SINGLE_ATTRIBUTE_VALUE;
                    } else if (c == '-') {
                        emit(c);
                    } else if (isControlChar(c)) {
                        emit(c);
                    } else if (Character.isWhitespace(c)) {
                        buffer.clear();
                    } else {
                        state = State.IN_STRING;
                        buffer.put(b);
                    }
                } else if (state == State.IN_STRING) {
                    if (c == '>') {
                        emitBuffer();
                        emit(c);
                        state = State.START;
                    } else if (isControlChar(c)) {
                        emitBuffer();
                        emit(c);
                        state = State.IN_TAG;
                    } else if (Character.isWhitespace(c)) {
                        emitBuffer();
                        state = State.IN_TAG;
                    } else {
                        buffer.put(b);
                    }
                } else if (state == State.IN_DOUBLE_ATTRIBUTE_VALUE) {
                    if (c == '"') {
                        emitBuffer();
                        emit(c);
                        state = State.IN_TAG;
                    } else {
                        buffer.put(b);
                    }
                } else if (state == State.IN_SINGLE_ATTRIBUTE_VALUE) {
                    if (c == '\'') {
                        emitBuffer();
                        emit(c);
                        state = State.IN_TAG;
                    } else {
                        buffer.put(b);
                    }
                }

            } catch (Exception exc) {
                throw new BadFormatError("Unable to parse XML, next byte is " + b + " ('"
                        + StringEscapeUtils.escapeJava("" + c) + "'); exc: " + exc, exc);
            }
        }
    }

    public void restart() {
        buffer.clear();
    }

    private enum State {
        CLOSED, IN_DOUBLE_ATTRIBUTE_VALUE, IN_SINGLE_ATTRIBUTE_VALUE, IN_STRING, IN_TAG, IN_TEXT, START
    }

    public static interface TokenListener {
        void token(char c, CharSequence token) throws BadFormatError;
    }
}
