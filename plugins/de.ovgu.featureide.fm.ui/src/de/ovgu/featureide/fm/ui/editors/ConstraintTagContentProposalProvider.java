/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2017  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 *
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://featureide.cs.ovgu.de/ for further information.
 */
package de.ovgu.featureide.fm.ui.editors;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;

/**
 * Provides proposals for content assist while typing constraint tags
 *
 * @author Rahel Arens
 */
public class ConstraintTagContentProposalProvider implements IContentProposalProvider {

	Set<String> constraintTags;

	public ConstraintTagContentProposalProvider(Set<String> tags) {
		constraintTags = tags;
	}

	@Override
	public IContentProposal[] getProposals(String contents, int position) {
		final List<ContentProposal> proposalList = new ArrayList<ContentProposal>();

		// if nothing is entered yet, propose all tags that does not yet exist for this constraint
		if ("".equals(contents)) {
			for (final String tag : constraintTags) {
				proposalList.add(new ContentProposal(tag));
			}
		} else {
			// filter proposal to only propose tags that still match the entered string
			for (final String tag : constraintTags) {
				if ((tag.length() >= contents.length()) && tag.substring(0, contents.length()).equalsIgnoreCase(contents)) {
					proposalList.add(new ContentProposal(tag));
				}
			}
		}

		return proposalList.toArray(new IContentProposal[proposalList.size()]);
	}

}
