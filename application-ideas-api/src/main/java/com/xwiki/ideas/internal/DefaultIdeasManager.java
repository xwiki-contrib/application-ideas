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
package com.xwiki.ideas.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.ideas.IdeasException;
import com.xwiki.ideas.IdeasManager;
import com.xwiki.ideas.model.Idea;

/**
 * @version $Id$
 * @since 1.14
 */
@Component
@Singleton
public class DefaultIdeasManager implements IdeasManager
{
    static final String VOTERS_AGAINST_KEY = "against";

    static final String NUMBER_OF_AGAINST_VOTES_KEY = "nbagainst";

    static final String VOTERS_FOR_KEY = "supporters";

    static final String NUMBER_OF_FOR_VOTES_KEY = "nbvotes";

    private static final String IDEAS_SPACE = "Ideas";

    private static final String CODE_SPACE = "Code";

    private static final LocalDocumentReference IDEA_CLASS_REFERENCE =
        new LocalDocumentReference(IDEAS_SPACE, "IdeasClass");

    private static final LocalDocumentReference IDEA_STATUS_CLASS_REFERENCE =
        new LocalDocumentReference(List.of(IDEAS_SPACE, CODE_SPACE), "StatusClass");

    private static final Map<String, String> VOTER_KEY_TO_NR_KEY = new HashMap<>();

    private static final String NOT_FOUND_ERROR =
        "The document [%s] does not exists or it does not have an Idea Object.";

    private static final String FAILED_LOAD_EXCEPTION = "Failed to load idea document [%s].";

    private static final String COMMA = ",";

    static {
        VOTER_KEY_TO_NR_KEY.put(VOTERS_AGAINST_KEY, NUMBER_OF_AGAINST_VOTES_KEY);
        VOTER_KEY_TO_NR_KEY.put(VOTERS_FOR_KEY, NUMBER_OF_FOR_VOTES_KEY);
    }

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private Logger logger;

    @Override
    public Idea vote(DocumentReference documentReference, boolean pro)
        throws IdeasException
    {
        return
            performIdeaActions(
                documentReference,
                pro,
                (ideasObj, serializedUser, xcontext) -> {
                    addVote(VOTERS_FOR_KEY, ideasObj, serializedUser, xcontext);
                    removeVote(VOTERS_AGAINST_KEY, ideasObj, serializedUser, xcontext);
                },
                (ideasObj, serializedUser, xcontext) -> {
                    addVote(VOTERS_AGAINST_KEY, ideasObj, serializedUser, xcontext);
                    removeVote(VOTERS_FOR_KEY, ideasObj, serializedUser, xcontext);
                }
            );
    }

    @Override
    public Idea removeVote(DocumentReference documentReference) throws IdeasException
    {
        return performIdeaActions(
            documentReference,
            null,
            (ideaObj, user, xcontext) -> removeVote(VOTERS_FOR_KEY, ideaObj, user, xcontext),
            (ideaObj, user, xcontext) -> removeVote(VOTERS_AGAINST_KEY, ideaObj, user, xcontext)
        );
    }

    @Override
    public Idea get(DocumentReference documentReference) throws IdeasException
    {
        try {
            XWikiContext xcontext = contextProvider.get();
            XWiki xWiki = xcontext.getWiki();
            XWikiDocument ideasDoc = xWiki.getDocument(documentReference, xcontext);
            BaseObject ideasObj = ideasDoc.getXObject(IDEA_CLASS_REFERENCE);

            List<String> proUsers = getListValue(VOTERS_FOR_KEY, ideasObj);
            List<String> againstUsers = getListValue(VOTERS_AGAINST_KEY, ideasObj);
            Idea result = new Idea();
            result.getOpponents().addAll(againstUsers);
            result.getSupporters().addAll(proUsers);
            return result;
        } catch (XWikiException e) {
            throw new IdeasException(NOT_FOUND_ERROR, e);
        }
    }

    @Override
    public boolean exists(DocumentReference documentReference) throws IdeasException
    {
        try {
            XWikiContext xcontext = contextProvider.get();
            XWiki xWiki = xcontext.getWiki();
            XWikiDocument ideasDoc = xWiki.getDocument(documentReference, xcontext);
            BaseObject ideasObject = ideasDoc.getXObject(IDEA_CLASS_REFERENCE);
            return ideasObject != null;
        } catch (XWikiException e) {
            throw new IdeasException(String.format(FAILED_LOAD_EXCEPTION, documentReference), e);
        }
    }

    @Override
    public boolean isOpenToVote(String status)
    {
        try {
            XWikiContext xcontext = contextProvider.get();
            XWiki xWiki = xcontext.getWiki();
            XWikiDocument ideasStatusDoc = xWiki.getDocument(
                new LocalDocumentReference(List.of(IDEAS_SPACE, CODE_SPACE, "Statuses"), "Status_" + status), xcontext);
            BaseObject ideasStatusObject = ideasStatusDoc.getXObject(IDEA_STATUS_CLASS_REFERENCE);
            if (ideasStatusObject == null) {
                return false;
            } else {
                return ideasStatusObject.getIntValue("openToVote") == 1;
            }
        } catch (XWikiException e) {
            logger.warn("Failed to retrieve the openToVote property for the idea status [{}]. Root cause is: [{}]",
                status, ExceptionUtils.getRootCauseMessage(e));
            return false;
        }
    }

    private Idea performIdeaActions(DocumentReference documentReference, Boolean pro, IdeaAction proAction,
        IdeaAction againstAction) throws IdeasException
    {
        XWikiContext xcontext = contextProvider.get();
        XWiki xWiki = xcontext.getWiki();
        try {
            XWikiDocument ideasDoc = xWiki.getDocument(documentReference, xcontext);

            BaseObject ideasObj = ideasDoc.getXObject(IDEA_CLASS_REFERENCE);
            DocumentReference user = xcontext.getUserReference();
            if (null != ideasObj) {
                String serializedUser = serializer.serialize(user, documentReference.getWikiReference());

                if (pro == null) {
                    proAction.perform(ideasObj, serializedUser, xcontext);
                    againstAction.perform(ideasObj, serializedUser, xcontext);
                } else if (pro) {
                    proAction.perform(ideasObj, serializedUser, xcontext);
                } else {
                    againstAction.perform(ideasObj, serializedUser, xcontext);
                }
                Idea result = get(documentReference);

                // Save document
                xWiki.saveDocument(ideasDoc, "Updated Votes", xcontext);
                return result;
            } else {
                throw new IdeasException(String.format(NOT_FOUND_ERROR, documentReference));
            }
        } catch (XWikiException e) {
            throw new IdeasException(String.format(FAILED_LOAD_EXCEPTION, documentReference));
        }
    }

    private void addVote(String voterKey, BaseObject ideasObj, String user, XWikiContext xcontext)
    {
        Set<String> userSet = new HashSet<>(getListValue(voterKey, ideasObj));

        userSet.add(user);

        ideasObj.set(voterKey, StringUtils.join(userSet, COMMA), xcontext);
        ideasObj.set(VOTER_KEY_TO_NR_KEY.get(voterKey), userSet.size(), xcontext);
    }

    private void removeVote(String voterKey, BaseObject ideasObj, String user, XWikiContext xcontext)
    {
        Set<String> userSet = new HashSet<>(getListValue(voterKey, ideasObj));
        userSet.remove(user);
        ideasObj.set(voterKey, StringUtils.join(userSet, COMMA), xcontext);
        ideasObj.set(VOTER_KEY_TO_NR_KEY.get(voterKey), userSet.size(), xcontext);
    }

    private List<String> getListValue(String voterKey, BaseObject ideasObj)
    {
        String userListAsString = ideasObj.getStringValue(voterKey);
        List<String> userList;
        if (userListAsString.isEmpty()) {
            userList = new ArrayList<>();
        } else {
            userList = new ArrayList<>(Arrays.asList(userListAsString.split(COMMA)));
        }
        return userList;
    }
}
