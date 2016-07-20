/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2016  FeatureIDE team, University of Magdeburg, Germany
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
package de.ovgu.featureide.fm.ui.wizards;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import de.ovgu.featureide.fm.core.ConstraintAttribute;
import de.ovgu.featureide.fm.core.FeatureModelAnalyzer;
import de.ovgu.featureide.fm.core.base.FeatureUtils;
import de.ovgu.featureide.fm.core.base.IConstraint;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.explanations.Redundancy;
import de.ovgu.featureide.fm.ui.editors.FeatureDiagramEditor;
import de.ovgu.featureide.fm.ui.editors.FeatureModelEditor;
import de.ovgu.featureide.fm.ui.editors.IGraphicalFeatureModel;

/**
 * A wizard page to show implicit constraints of a sub feature model. Enables automated analysis
 * on the sub feature model to explain implicit (redundant) constraints.
 * 
 * @author "Ananieva Sofia"
 */
public class SubtreeDependencyPage extends AbstractWizardPage {

	/**
	 * The sub feature model which contains implicit constraints if there are any.
	 */
	private IFeatureModel subtreeModel;

	/**
	 * The origin feature model which contains the sub feature model.
	 */
	private IFeatureModel oldFm;

	/**
	 * Remember explanation for redundant constraint. Key = constraintIndex, Value = explanation.
	 * Used as tool tip for redundant constraint.
	 */
	public static HashMap<Integer, List<String>> redundantExpl = new HashMap<Integer, List<String>>();

	/**
	 * Constructor.
	 * 
	 * @param fm The sub feature model
	 * @param oldModel The origin feature model
	 */
	public SubtreeDependencyPage(IFeatureModel fm, IFeatureModel oldModel) {
		super("Sub Feature Model Dependencies");
		setTitle("Sub Feature Model Dependencies");
		setDescription("Here you see implicit dependencies of the selected feature model, if there are any.");
		subtreeModel = fm;
		oldFm = oldModel;
	}

	/**
	 * Creates a control for the dialog page. Integrates the sub feature model and uses FillLayout to
	 * fill all available space.
	 * 
	 * @param parent A composite which contains the feature model
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new FillLayout());
		setControl(container);
		insertFeatureModel(container);
		setPageComplete(false);
	}

	/**
	 * Inserts the subtree model into a container within a wizard page.
	 * Enables automated analysis for the sub feature model and explains implicit constraints
	 * using the origin feature model.
	 * 
	 * @param comp A composite which contains the sub feature model
	 */
	private void insertFeatureModel(Composite comp) {
		FeatureModelAnalyzer analyzer = subtreeModel.getAnalyser();

		FeatureModelEditor modeleditor = new FeatureModelEditor();
		modeleditor.setFeatureModel(subtreeModel);
		FeatureDiagramEditor diagramEditor = new FeatureDiagramEditor(modeleditor, comp, subtreeModel);
		subtreeModel.addListener(diagramEditor);
		
		analyzer.calculateFeatures = oldFm.getAnalyser().calculateFeatures;
		analyzer.calculateConstraints = oldFm.getAnalyser().calculateConstraints;
		analyzer.calculateRedundantConstraints = oldFm.getAnalyser().calculateRedundantConstraints;
		analyzer.calculateTautologyConstraints = oldFm.getAnalyser().calculateTautologyConstraints;
		analyzer.calculateDeadConstraints = oldFm.getAnalyser().calculateDeadConstraints;
		analyzer.calculateFOConstraints = oldFm.getAnalyser().calculateFOConstraints;

		analyzer.analyzeFeatureModel(null); // analyze the subtree model
		explainImplicitConstraints(analyzer, diagramEditor.getGraphicalFeatureModel()); // explain implicit, i.e. redundant, constraints
		diagramEditor.setContents(diagramEditor.getGraphicalFeatureModel());

		diagramEditor.internRefresh(true);
		diagramEditor.getGraphicalFeatureModel().redrawDiagram();
	}

	/**
	 * Checks if implicit constraints are redundant (must be) and explains them.
	 * Sets the tool tip with explanations.
	 * 
	 * @param analyzer The feature model analyzer for the sub feature model
	 * @param graphicalFeatModel The graphical feature model of the sub feature model
	 */
	private void explainImplicitConstraints(FeatureModelAnalyzer analyzer, IGraphicalFeatureModel graphicalFeatModel) {
		// iterate implicit constraints and generate explanations 
		for (IConstraint redundantC : getImplicitConstraints()) {
			List<String> expl = new Redundancy().explain(oldFm, subtreeModel, redundantC);
			subtreeModel.getAnalyser().redundantConstrExpl.put(FeatureUtils.getConstraintIndex(subtreeModel, redundantC), expl);
			redundantC.setConstraintAttribute(ConstraintAttribute.IMPLICIT, false);
		}
	}

	/**
	 * Returns implicit constraints for the sub feature model by iterating constraints of the
	 * origin model and the sub feature model and collecting constraints which are only present
	 * in the sub feature model but not in the origin one.
	 * 
	 * @return result A list which contains implicit constraints for the subtree feature model
	 */
	private List<IConstraint> getImplicitConstraints() {
		List<IConstraint> newConstraints = subtreeModel.getConstraints();
		List<IConstraint> oldConstraints = oldFm.getConstraints();
		List<IConstraint> result = new ArrayList<>();
		result.addAll(newConstraints);

		Iterator<IConstraint> it = result.iterator();
		while (it.hasNext()) {
			IConstraint constrNew = it.next();
			for (IConstraint constrOld : oldConstraints) {
				if (constrOld.getNode().toRegularCNF().equals(constrNew.getNode().toRegularCNF())) {
					it.remove();
					break;
				}
			}
		}
		return result;
	}

	@Override
	public void putData() {
		abstractWizard.putData(WizardConstants.KEY_IN_FEATUREMODEL, subtreeModel);
	}

}