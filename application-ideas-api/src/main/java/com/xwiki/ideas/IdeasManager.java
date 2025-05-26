/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xwiki.ideas;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

import com.xwiki.ideas.model.Idea;

/**
 * Provides methods to interact with Ideas.
 *
 * @version $Id$
 * @since 1.14
 */
@Role
@Unstable
public interface IdeasManager
{
    /**
     * Registers a vote for an Idea on the behalf of the current user. The vote can be either for or against.
     *
     * @param documentReference a reference to a document that contains an Idea
     * @param pro true if the user agrees with the Idea; false otherwise
     * @return an Object representation of the vote result
     * @throws IdeasException if the document does not exist or it doesn't have an Idea
     */
    Idea vote(DocumentReference documentReference, boolean pro) throws IdeasException;

    /**
     * @param documentReference a reference to a document that contains an Idea
     * @return an Object representation of the Idea after the vote has been removed
     * @throws IdeasException if the document does not exist or it doesn't have an Idea
     */
    Idea removeVote(DocumentReference documentReference) throws IdeasException;

    /**
     * Retrieves the state of an Idea.
     *
     * @param documentReference a reference to a document that contains an Idea
     * @return an Object representation of the Idea
     * @throws IdeasException if the document does not exist or it doesn't have an Idea
     */
    Idea get(DocumentReference documentReference) throws IdeasException;

    /**
     * A function to check whether an Idea exists or not.
     *
     * @param documentReference a document reference that we want to check
     * @return true if the document has an Idea; false otherwise
     * @throws IdeasException if the document does not exists or it doesn't have an Idea
     */
    boolean exists(DocumentReference documentReference) throws IdeasException;

    /**
     * A function to see if a status allows voting on an Idea.
     *
     * @param status the name of the status to check
     * @return whether the status allows voting on an Idea
     * @since 1.16
     */
    boolean isOpenToVote(String status) throws IdeasException;
}
