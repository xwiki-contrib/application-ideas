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
package com.xwiki.ideas.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import com.xwiki.ideas.IdeasException;
import com.xwiki.ideas.IdeasManager;

/**
 * Script service for retrieving information about the Ideas Application.
 *
 * @version $Id$
 * @since 1.16
 */
@Component
@Named("ideas")
@Singleton
@Unstable
public class IdeasScriptService implements ScriptService
{
    @Inject
    private IdeasManager ideasManager;

    /**
     * Check if an idea with the given status allows voting.
     *
     * @param status the status to check
     * @return true if an idea with the given status is open for voting, false otherwise
     */
    public boolean isOpenToVote(String status) throws IdeasException
    {
        return ideasManager.isOpenToVote(status);
    }
}
