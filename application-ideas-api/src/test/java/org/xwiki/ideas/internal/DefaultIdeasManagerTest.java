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
package org.xwiki.ideas.internal;

import java.util.Arrays;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import org.xwiki.ideas.IdeasException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultIdeasManager}.
 *
 * @version $Id$
 * @since 1.10
 */
@ComponentTest
class DefaultIdeasManagerTest
{
    private static final LocalDocumentReference IDEA_CLASS_REFERENCE =
        new LocalDocumentReference("Ideas", "IdeasClass");

    private static final String XWIKI = "XWiki";

    private static final String SPACE_1 = "Space1";

    private static final String SPACE_2 = "Space2";

    private static final String PAGE = "Page";

    @InjectMockComponents
    private DefaultIdeasManager manager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    @Named("compactwiki")
    private EntityReferenceSerializer<String> serializer;

    @Mock
    private XWikiContext xWikiContext;

    @Mock
    private XWiki wiki;
    @Mock
    private XWikiDocument document;

    @BeforeEach
    void setup()
    {
        when(this.contextProvider.get()).thenReturn(this.xWikiContext);
        when(this.xWikiContext.getWiki()).thenReturn(this.wiki);
    }

    @Test
    void voteDocumentWithNoIdeaObjectTest() throws XWikiException
    {
        DocumentReference input = new DocumentReference(XWIKI, Arrays.asList(SPACE_1, SPACE_2), PAGE);

        when(this.document.isNew()).thenReturn(false);
        when(this.document.clone()).thenReturn(this.document);
        when(this.wiki.getDocument(input, this.xWikiContext)).thenReturn(this.document);

        IdeasException e = assertThrows(IdeasException.class, () -> this.manager.vote(input, true));
        assertTrue(e.getMessage().contains(input.toString()));
    }

    @Test
    void voteProWhenThereAreNoOtherVotesTest() throws XWikiException, IdeasException
    {
        String userName = "XWiki:Space1.User";
        DocumentReference user = new DocumentReference(XWIKI, SPACE_1, "User");

        DocumentReference input = new DocumentReference(XWIKI, Arrays.asList(SPACE_1, SPACE_2), PAGE);
        BaseObject ideaObj = mock(BaseObject.class);
        when(this.wiki.getDocument(input, this.xWikiContext)).thenReturn(this.document);
        when(this.document.getXObject(IDEA_CLASS_REFERENCE)).thenReturn(ideaObj);
        when(this.xWikiContext.getUserReference()).thenReturn(user);
        when(this.document.isNew()).thenReturn(false);
        when(this.document.clone()).thenReturn(this.document);
        when(this.serializer.serialize(user, input.getWikiReference())).thenReturn(userName);
        when(ideaObj.getStringValue(DefaultIdeasManager.VOTERS_FOR_KEY)).thenReturn("");
        when(ideaObj.getStringValue(DefaultIdeasManager.VOTERS_AGAINST_KEY)).thenReturn("");
        this.manager.vote(input, true);

        verify(this.wiki).saveDocument(this.document, "Updated Votes", this.xWikiContext);
    }

    @Test
    void isOpenToVoteTest() throws XWikiException
    {
        BaseObject statusObj = mock(BaseObject.class);
        when(statusObj.getIntValue("openToVote")).thenReturn(1);
        when(this.wiki.getDocument(any(EntityReference.class), any())).thenReturn(this.document);
        when(this.document.getXObject(any(EntityReference.class))).thenReturn(statusObj);

        assertTrue(this.manager.isOpenToVote("open"));
    }
}
