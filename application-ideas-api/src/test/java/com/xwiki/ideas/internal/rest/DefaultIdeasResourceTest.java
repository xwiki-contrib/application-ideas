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
package com.xwiki.ideas.internal.rest;

import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiContext;
import com.xwiki.ideas.IdeasException;
import com.xwiki.ideas.IdeasManager;
import com.xwiki.ideas.model.Idea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultIdeasResource}.
 *
 * @version $Id$
 * @since 1.14
 */
@ComponentTest
public class DefaultIdeasResourceTest
{
    @InjectMockComponents
    private DefaultIdeasResource ideasResource;

    @MockComponent
    private IdeasManager manager;

    @MockComponent
    private ContextualAuthorizationManager authorizationManager;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    protected Provider<XWikiContext> xcontextProvider;

    private XWikiContext xWikiContext;

    @BeforeComponent
    public void configure() throws Exception
    {
        ComponentManager contextComponentManager =
            this.componentManager.registerMockComponent(ComponentManager.class, "context");
        Execution execution = mock(Execution.class);
        when(contextComponentManager.getInstance(Execution.class)).thenReturn(execution);
        ExecutionContext executionContext = new ExecutionContext();
        this.xWikiContext = mock(XWikiContext.class);
        executionContext.setProperty("xwikicontext", this.xWikiContext);
        when(execution.getContext()).thenReturn(executionContext);
        when(this.xcontextProvider.get()).thenReturn(this.xWikiContext);
    }

    @Test
    void castVoteTest() throws IdeasException, XWikiRestException
    {
        int nOfVotes = 1;
        Idea voteResult = new Idea();
        voteResult.getSupporters().add("");
        when(authorizationManager.hasAccess(any(), any())).thenReturn(true);
        when(manager.exists(any())).thenReturn(true);
        when(manager.vote(any(), anyBoolean())).thenReturn(voteResult);
        com.xwiki.ideas.model.jaxb.Idea response = this.ideasResource.vote("wiki", "space", "page", "true");

        assertEquals(nOfVotes, response.getSupporters().size());
    }
}
