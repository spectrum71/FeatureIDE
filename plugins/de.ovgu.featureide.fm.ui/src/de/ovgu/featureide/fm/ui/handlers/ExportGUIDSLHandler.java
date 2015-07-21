/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2015  FeatureIDE team, University of Magdeburg, Germany
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
package de.ovgu.featureide.fm.ui.handlers;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import de.ovgu.featureide.fm.core.FeatureModel;
import de.ovgu.featureide.fm.core.io.IFeatureModelWriter;
import de.ovgu.featureide.fm.core.io.guidsl.GuidslWriter;
import de.ovgu.featureide.fm.ui.handlers.base.AbstractExportHandler;

/**
 * Exports feature model to GUIDSL format.
 * 
 * @author Sebastian Krieter
 */
public class ExportGUIDSLHandler extends AbstractExportHandler {

	@Override
	protected IFeatureModelWriter getFeatureModelWriter(FeatureModel fm) {
		final GuidslWriter fmWriter = new GuidslWriter(fm);
		if (fmWriter.hasConcreteCompounds()
				&& !MessageDialog.openQuestion(new Shell(), "Warning!",
						"The current feature model cannot be transformed due to concrete compounds! Proceed? (all compound features will be set as abstract)")) {
			return null;
		}
		return fmWriter;
	}

	@Override
	protected void configureFileDialog(FileDialog fileDialog) {
		super.configureFileDialog(fileDialog);
		fileDialog.setFileName("model.m");
		fileDialog.setFilterExtensions(new String[] { "*.m" });
		fileDialog.setFilterNames(new String[] { "GUIDSL foramt *.m" });
	}

}