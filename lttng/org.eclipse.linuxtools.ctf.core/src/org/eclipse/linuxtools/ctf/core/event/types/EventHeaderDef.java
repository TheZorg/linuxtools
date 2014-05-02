package org.eclipse.linuxtools.ctf.core.event.types;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.linuxtools.ctf.core.event.scope.LexicalScope;

/**
 * An event header definition, as shown in the example of the ctf spec
 *
 * @author Matthew Khouzam
 * @since 3.0
 *
 */
public final class EventHeaderDef extends Definition {

    /**
     *
     */
    private final int fId;
    private final long fTimestamp;
    private final int fTimestampLength;

    /**
     * Event header defintion
     *
     * @param id
     *            the event id
     * @param timestamp
     *            the timestamp
     * @param eventHeaderCompactDecl
     *            The declaration of this defintion
     * @param timestampLength
     *            the number of bits valid in the timestamp
     */
    public EventHeaderDef(@NonNull Declaration eventHeaderCompactDecl, int id, long timestamp, int timestampLength) {
        super(eventHeaderCompactDecl, null, "event.header", LexicalScope.EVENT_HEADER); //$NON-NLS-1$
        fId = id;
        fTimestamp = timestamp;
        fTimestampLength = timestampLength;
    }

    /**
     * Gets the timestamp declaration
     *
     * @return the timestamp declaration
     */
    public int getTimestampLength() {
        return fTimestampLength;
    }

    /**
     * Get the event id
     *
     * @return the event id
     */
    public int getId() {
        return fId;
    }

    /**
     * Get the timestamp
     *
     * @return the timestamp
     */
    public long getTimestamp() {
        return fTimestamp;
    }
}