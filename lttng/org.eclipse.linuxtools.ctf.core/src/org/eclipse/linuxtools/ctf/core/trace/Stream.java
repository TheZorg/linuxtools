/*******************************************************************************
 * Copyright (c) 2011, 2014 Ericsson, Ecole Polytechnique de Montreal and others
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Matthew Khouzam - Initial API and implementation
 * Contributors: Simon Marchi - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.ctf.core.trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.linuxtools.ctf.core.event.IEventDeclaration;
import org.eclipse.linuxtools.ctf.core.event.types.Declaration;
import org.eclipse.linuxtools.ctf.core.event.types.StructDeclaration;
import org.eclipse.linuxtools.internal.ctf.core.event.EventDeclaration;
import org.eclipse.linuxtools.internal.ctf.core.event.metadata.exceptions.ParseException;

import com.google.common.collect.ImmutableList;

/**
 * <b><u>Stream</u></b>
 * <p>
 * Represents a stream in a trace.
 * @since 2.0
 */
public class Stream {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------


    /**
     * The numerical ID of the stream
     */
    private Long fId = null;

    /**
     * Declarations of the stream-specific structures
     */
    private StructDeclaration fPacketContextDecl = null;
    private Declaration fEventHeaderDecl = null;
    private StructDeclaration fEventContextDecl = null;

    /**
     * The trace to which the stream belongs
     */
    private CTFTrace fTrace = null;

    /**
     * Maps event ids to events
     */
    private List< IEventDeclaration> fEvents = Collections.EMPTY_LIST;

    /**
     * The inputs associated to this stream
     */
    private final Set<StreamInput> fInputs = new HashSet<>();

    private IEventDeclaration fEventUnsetId;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Constructs a Stream that belongs to a Trace
     *
     * @param trace
     *            The trace to which belongs this stream.
     */
    public Stream(CTFTrace trace) {
        fTrace = trace;
    }

    // ------------------------------------------------------------------------
    // Getters/Setters/Predicates
    // ------------------------------------------------------------------------

    /**
     * Sets the id of a stream
     * @param id the id of a stream
     */
    public void setId(long id) {
        fId = id;
    }

    /**
     * Gets the id of a stream
     * @return id the id of a stream
     */
    public Long getId() {
        return fId;
    }

    /**
     * Is the id of a stream set
     *
     * @return If the ID is set or not
     */
    public boolean isIdSet() {
        return fId != null;
    }

    /**
     *
     * @return is the event header set (timestamp and stuff) (see Ctf Spec)
     */
    public boolean isEventHeaderSet() {
        return fEventHeaderDecl != null;
    }

    /**
    *
    * @return is the event context set (pid and stuff) (see Ctf Spec)
    */
    public boolean isEventContextSet() {
        return fEventContextDecl != null;
    }

    /**
     *
     * @return Is the packet context set (see Ctf Spec)
     */
    public boolean isPacketContextSet() {
        return fPacketContextDecl != null;
    }

    /**
     *
     * @param eventHeader the current event header for all events in this stream
     * @since 3.0
     */
    public void setEventHeader(Declaration eventHeader) {
        fEventHeaderDecl = eventHeader;
    }

    /**
     *
     * @param eventContext the context for all events in this stream
     */
    public void setEventContext(StructDeclaration eventContext) {
        fEventContextDecl = eventContext;
    }

    /**
     *
     * @param packetContext the packet context for all packets in this stream
     */
    public void setPacketContext(StructDeclaration packetContext) {
        fPacketContextDecl = packetContext;
    }

    /**
     *
     * @return the event header declaration in structdeclaration form
     * @since 3.0
     */
    public Declaration getEventHeaderDecl() {
        return fEventHeaderDecl;
    }

    /**
     *
     * @return the event context declaration in structdeclaration form
     */
    public StructDeclaration getEventContextDecl() {
        return fEventContextDecl;
    }

    /**
     *
     * @return the packet context declaration in structdeclaration form
     */
    public StructDeclaration getPacketContextDecl() {
        return fPacketContextDecl;
    }

    /**
     *
     * @return the set of all stream inputs for this stream
     */
    public Set<StreamInput> getStreamInputs() {
        return fInputs;
    }

    /**
     *
     * @return the parent trace
     */
    public CTFTrace getTrace() {
        return fTrace;
    }

    /**
     * Get the event declarations of this stream
     *
     * @return all the event declarations for this stream, using the id as a key
     *         for the hashmap.
     * @since 3.0
     */
    public List<IEventDeclaration> getEvents() {
        return fEvents;
    }


    /**
     * Add a list of event declarations to this stream
     * @param events list of the events to add
     * @since 3.0
     */
    public void addEvents( List<IEventDeclaration> events){
        if( fEvents == Collections.EMPTY_LIST ) {
            fEvents = ImmutableList.<IEventDeclaration>copyOf(events);
        }else{
            // out of order, thanks Obama!
            // this should not happen though, like ever
            fEvents = new ImmutableList.Builder<IEventDeclaration>()
                    .addAll(fEvents)
                    .addAll(events)
                    .build();
        }
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    /**
     * Adds an event to the event map.
     *
     * An event in a stream can omit its id if it is the only event in this
     * stream. An event for which no id has been specified has a null id. It is
     * thus not possible to add an event with the null key if the map is not
     * empty. It is also not possible to add an event to the map if the null key
     * is present in the map.
     *
     * @param event
     *            The event to add
     * @throws ParseException
     *             If there was a problem reading the event or adding it to the
     *             stream
     */
    public void addEvent(IEventDeclaration event) throws ParseException {

        if( fEvents == Collections.EMPTY_LIST ) {
            fEvents = new ArrayList<>();
        }
        int id = event.getId();
        if( id == EventDeclaration.UNSET_EVENT_ID && fEventUnsetId != null){
            throw new ParseException(
                    "Event without id with multiple events in a stream"); //$NON-NLS-1$
        }
        while( fEvents.size() <= id){
            fEvents.add(null);
        }
        /* Check if an event with the same ID already exists */
        if (fEvents.get(id) != null) {
            throw new ParseException("Event id already exists"); //$NON-NLS-1$
        }
        if (id == EventDeclaration.UNSET_EVENT_ID) {
            fEventUnsetId = event;
        } else {
            /* Put the event in the map */
            if( fEvents.size() < id){
                fEvents = new ArrayList<>(fEvents);
                while(fEvents.size() < id){
                    fEvents.add(null);
                }
            }
            fEvents.set(id, event);
        }
    }

    /**
     * Add an input to this Stream
     *
     * @param input
     *            The StreamInput to add.
     */
    public void addInput(StreamInput input) {
        fInputs.add(input);
    }

    @Override
    public String toString() {
        return "Stream [id=" + fId + ", packetContextDecl=" + fPacketContextDecl //$NON-NLS-1$ //$NON-NLS-2$
                + ", eventHeaderDecl=" + fEventHeaderDecl //$NON-NLS-1$
                + ", eventContextDecl=" + fEventContextDecl + ", trace=" + fTrace //$NON-NLS-1$ //$NON-NLS-2$
                + ", events=" + fEvents + ", inputs=" + fInputs + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
}
