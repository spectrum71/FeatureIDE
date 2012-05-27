/* FeatureIDE - An IDE to support feature-oriented software development
 * Copyright (C) 2005-2011  FeatureIDE Team, University of Magdeburg
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 * See http://www.fosd.de/featureide/ for further information.
 */
package de.ovgu.featureide.core.typecheck.check;

import AST.ASTNode;
import de.ovgu.featureide.fm.core.Feature;
import de.ovgu.featureide.fm.core.FeatureModel;

/**
 * TODO description
 * 
 * @author S�nke Holthusen
 */
public interface ICheckPlugin
{
    /**
     * Registers with the Plug-in Manager for checks
     * 
     * @param manager the plug-in manager
     */
	public void register(CheckPluginManager manager);

	/**
	 * Will be called before invoking the check itself
	 * can be used to initialize data structures using the collected ASTNodes
	 */
	public void init();
	
	/**
	 * Will be called by the plug-in manager after {@link ICheckPlugin#init()}
	 * 
	 * @param fm the feature model for the software product line to be checked
	 */
	public void invokeCheck(FeatureModel fm);
	
	/**
	 * Will be called by the plug-in manager while parsing the AST
	 * when registered for the specific ASTNode
	 * 
	 * @param feature the feature the node belongs to
	 * @param node the current node
	 */
	public void invokeNodeParse(Feature feature, ASTNode node);
	
	/**
	 * Removes all data about the feature from the node list
	 * Will be called by the parser when a feature is parsed the first time or has to be reparsed
	 * 
	 * @param feature
	 */
	public void resetFeature(Feature feature);
	
	/**
	 * returns the name of the plug-in
	 * @return the name
	 */
	public String getName();
}
