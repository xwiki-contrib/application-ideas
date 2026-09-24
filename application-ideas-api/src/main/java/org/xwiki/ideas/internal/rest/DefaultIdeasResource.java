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
package org.xwiki.ideas.internal.rest;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.resources.pages.ModifiablePageResource;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import org.xwiki.ideas.IdeasException;
import org.xwiki.ideas.IdeasManager;
import org.xwiki.ideas.internal.IdeaMapper;
import org.xwiki.ideas.model.jaxb.Idea;
import org.xwiki.ideas.rest.IdeasResource;

/**
 * Default implementation of {@link IdeasResource}.
 *
 * @version $Id$
 * @since 1.10
 */
@Component
@Named("org.xwiki.ideas.internal.rest.DefaultIdeasResource")
@Singleton
public class DefaultIdeasResource extends ModifiablePageResource implements IdeasResource
{
    private static final LocalDocumentReference IDEA_CLASS_REFERENCE =
        new LocalDocumentReference("Ideas", "IdeasClass");

    @Inject
    private IdeasManager manager;

    @Inject
    private ContextualAuthorizationManager authorizationManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public Idea get(String xwikiName, String spaceName, String pageName) throws XWikiRestException
    {
        DocumentReference documentReference = new DocumentReference(pageName, getSpaceReference(spaceName, xwikiName));
        if (!this.authorizationManager.hasAccess(Right.VIEW, documentReference)) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
        try {
            return IdeaMapper.from(this.manager.get(documentReference));
        } catch (IdeasException e) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }

    @Override
    public Idea vote(String xwikiName, String spaceName, String pageName, String value)
        throws XWikiRestException
    {
        DocumentReference documentReference = new DocumentReference(pageName, getSpaceReference(spaceName, xwikiName));
        if (!this.authorizationManager.hasAccess(Right.EDIT, documentReference)) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
        try {
            if (this.manager.exists(documentReference)) {
                if (this.manager.isOpenToVote(getIdeaStatus(documentReference))) {
                    return IdeaMapper.from(this.manager.vote(documentReference, Boolean.valueOf(value)));
                } else {
                    throw new WebApplicationException(Response.Status.FORBIDDEN);
                }
            }
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } catch (IdeasException e) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Idea removeVote(String xwikiName, String spaceName, String pageName)
        throws XWikiRestException
    {
        DocumentReference documentReference = new DocumentReference(pageName, getSpaceReference(spaceName, xwikiName));
        if (!this.authorizationManager.hasAccess(Right.EDIT, documentReference)) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
        try {
            if (this.manager.exists(documentReference)) {
                if (this.manager.isOpenToVote(getIdeaStatus(documentReference))) {
                    return IdeaMapper.from(this.manager.removeVote(documentReference));
                } else {
                    throw new WebApplicationException(Response.Status.FORBIDDEN);
                }
            }
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } catch (IdeasException e) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private String getIdeaStatus(DocumentReference documentReference)
    {
        try {
            XWikiContext xcontext = this.contextProvider.get();
            XWiki xWiki = xcontext.getWiki();
            XWikiDocument ideasDoc = xWiki.getDocument(documentReference, xcontext);
            BaseObject ideasObject = ideasDoc.getXObject(IDEA_CLASS_REFERENCE);
            if (ideasObject == null) {
                return null;
            } else {
                return ideasObject.getStringValue("status");
            }
        } catch (XWikiException e) {
            getLogger().warn("Failed to retrieve the status for the idea on page [{}]. Root cause is: [{}]",
                documentReference, ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }
}
