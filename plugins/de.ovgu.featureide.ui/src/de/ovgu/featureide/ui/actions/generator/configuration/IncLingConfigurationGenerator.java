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
package de.ovgu.featureide.ui.actions.generator.configuration;

import java.util.List;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.fm.core.analysis.cnf.CNF;
import de.ovgu.featureide.fm.core.analysis.cnf.generator.PairWiseConfigurationGenerator;
import de.ovgu.featureide.fm.core.configuration.Selection;
import de.ovgu.featureide.fm.core.job.LongRunningWrapper;
import de.ovgu.featureide.fm.core.job.monitor.IMonitor;
import de.ovgu.featureide.ui.actions.generator.ConfigurationBuilder;

/**
 * Executed the IncLing pairwise sorting algorithm to create configurations.
 * 
 * @see PairWiseConfigurationGenerator
 * 
 * @author Jens Meinicke
 */
public class IncLingConfigurationGenerator extends AConfigurationGenerator {

	public IncLingConfigurationGenerator(ConfigurationBuilder builder, IFeatureProject featureProject) {
		super(builder, featureProject);
	}

	@Override
	public Void execute(IMonitor monitor) throws Exception {
		exec(cnf, getGenerator(), monitor);
		return null;
	}
	
	protected PairWiseConfigurationGenerator getGenerator() {
		return new PairWiseConfigurationGenerator(cnf, (int) builder.configurationNumber);
	}

	protected void exec(final CNF satInstance, final PairWiseConfigurationGenerator as, IMonitor monitor) {
		final Thread consumer = new Thread() {
			@Override
			public void run() {
				int foundConfigurations = 0;
				while (true) {
					try {
						generateConfiguration(satInstance.getVariables().convertToString(as.q.take().getModel()));
						foundConfigurations++;
					} catch (InterruptedException e) {
						break;
					}
				}
				foundConfigurations += as.q.size();
				builder.configurationNumber = foundConfigurations;
				for (PairWiseConfigurationGenerator.Configuration c : as.q) {
					generateConfiguration(satInstance.getVariables().convertToString(c.getModel()));
				}
			}

			private void generateConfiguration(List<String> solution) {
				configuration.resetValues();
				for (final String selection : solution) {
					configuration.setManual(selection, Selection.SELECTED);
				}
				addConfiguration(configuration);
			}
		};
		consumer.start();
		LongRunningWrapper.runMethod(as, monitor);
		consumer.interrupt();
	}

}
