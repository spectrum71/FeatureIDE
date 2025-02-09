/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2019  FeatureIDE team, University of Magdeburg, Germany
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
package de.ovgu.featureide.fm.core.job;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.ovgu.featureide.fm.core.analysis.cnf.CNF;
import de.ovgu.featureide.fm.core.analysis.cnf.CNFCreator;
import de.ovgu.featureide.fm.core.analysis.cnf.IVariables;
import de.ovgu.featureide.fm.core.analysis.cnf.LiteralSet;
import de.ovgu.featureide.fm.core.analysis.cnf.Nodes;
import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula;
import de.ovgu.featureide.fm.core.analysis.cnf.manipulator.remove.CNFSlicer;
import de.ovgu.featureide.fm.core.analysis.cnf.solver.SimpleSatSolver;
import de.ovgu.featureide.fm.core.base.FeatureUtils;
import de.ovgu.featureide.fm.core.base.IConstraint;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.IFeatureModelFactory;
import de.ovgu.featureide.fm.core.base.IFeatureStructure;
import de.ovgu.featureide.fm.core.base.impl.FMFactoryManager;
import de.ovgu.featureide.fm.core.base.impl.FeatureModel;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import de.ovgu.featureide.fm.core.job.monitor.IMonitor;
import de.ovgu.featureide.fm.core.localization.StringTable;

/**
 * Create mpl interfaces.
 *
 * @author Sebastian Krieter
 * @author Marcus Pinnecke (Feature Interface)
 */
public class SliceFeatureModel implements LongRunningMethod<IFeatureModel> {

	private static final int GROUP_OR = 1, GROUP_AND = 2, GROUP_ALT = 3, GROUP_NO = 0;
	private static final String MARK1 = "?", MARK2 = "??";
	private static final String ABSTRACT_NAME = "Abstract_";

	private boolean changed = false;
	private final boolean considerConstraints;
	private final FeatureModelFormula formula;
	private final Collection<String> featureNames;
	private final IFeatureModel featureModel;

	public SliceFeatureModel(IFeatureModel featureModel, Collection<String> featureNames, boolean considerConstraints) {
		this(featureModel, featureNames, considerConstraints, true);
	}

	public SliceFeatureModel(IFeatureModel featureModel, Collection<String> featureNames, boolean considerConstraints, boolean usePersistentFormula) {
		if (usePersistentFormula) {
			formula = FeatureModelManager.getInstance(featureModel).getPersistentFormula();
			this.featureModel = formula.getFeatureModel();
		} else {
			formula = FeatureModelManager.getInstance(featureModel).getVariableFormula();
			this.featureModel = featureModel;
		}
		this.featureNames = featureNames;
		this.considerConstraints = considerConstraints;
	}

	@Override
	public IFeatureModel execute(IMonitor<IFeatureModel> monitor) throws Exception {
		final IFeatureModelFactory factory = FMFactoryManager.getInstance().getFactory(featureModel);
		monitor.setRemainingWork(100);

		monitor.checkCancel();
		final CNF slicedFeatureModelCNF = sliceFormula(monitor.subTask(80));
		monitor.checkCancel();
		final IFeatureModel featureTree = sliceTree(featureNames, featureModel, factory, monitor.subTask(2));
		monitor.checkCancel();
		merge(factory, slicedFeatureModelCNF, featureTree, monitor.subTask(18));

		return featureTree;
	}

	private CNF sliceFormula(IMonitor<?> monitor) {
		monitor.setTaskName("Slicing Feature Model Formula");
		final ArrayList<String> removeFeatures = new ArrayList<>(FeatureUtils.getFeatureNames(featureModel));
		removeFeatures.removeAll(featureNames);
		return LongRunningWrapper.runMethod(new CNFSlicer(formula.getCNF(), removeFeatures), monitor.subTask(1));
	}

	private IFeatureModel sliceTree(Collection<String> selectedFeatureNames, IFeatureModel orgFeatureModel, IFeatureModelFactory factory, IMonitor<?> monitor) {
		monitor.setTaskName("Slicing Feature Tree");
		monitor.setRemainingWork(2);
		final IFeatureModel m = orgFeatureModel.clone();
		// mark features
		for (final IFeature feat : m.getFeatures()) {
			if (!selectedFeatureNames.contains(feat.getName())) {
				feat.setName(MARK1);
			}
		}

		final IFeature root = m.getStructure().getRoot().getFeature();

		m.getStructure().setRoot(null);

		// set new abstract root
		// needs to be done before resetting to get a unique ID
		final IFeature nroot = factory.createFeature(m, FeatureUtils.getFeatureName(orgFeatureModel, StringTable.DEFAULT_SLICING_ROOT_NAME));

		final long nextElementId = m.getNextElementId();
		m.reset();

		// keep the nextElementId so newly created abstract features get a unique ID
		if (m instanceof FeatureModel) {
			((FeatureModel) m).setNextElementId(nextElementId);
		}

		nroot.getStructure().setAbstract(true);
		nroot.getStructure().setAnd();
		nroot.getStructure().addChild(root.getStructure());
		root.getStructure().setParent(nroot.getStructure());

		// merge tree
		cut(nroot);
		do {
			changed = false;
			merge(nroot.getStructure(), GROUP_NO);
		} while (changed);
		monitor.step();

		// needed to correctly set unique names for newly created abstract features
		int abstractCount = 0;
		for (final IFeature f : orgFeatureModel.getFeatures()) {
			if (f.getName().startsWith(ABSTRACT_NAME)) {
				try {
					final int abstractNumber = Integer.parseInt(f.getName().substring(ABSTRACT_NAME.length()));
					if (abstractNumber >= abstractCount) {
						abstractCount = abstractNumber + 1;
					}
				} catch (final NumberFormatException e) {
					// feature name is not "Abstract_NUMBER". Can be ignored
				}
			}
		}

		final Hashtable<String, IFeature> featureTable = new Hashtable<>();
		final LinkedList<IFeature> featureStack = new LinkedList<>();
		featureStack.push(nroot);
		while (!featureStack.isEmpty()) {
			final IFeature curFeature = featureStack.pop();
			for (final IFeature feature : FeatureUtils.convertToFeatureList(curFeature.getStructure().getChildren())) {
				featureStack.push(feature);
			}
			if (curFeature.getName().startsWith(MARK1)) {
				curFeature.setName(ABSTRACT_NAME + abstractCount++);
				curFeature.getStructure().setAbstract(true);
			}
			featureTable.put(curFeature.getName(), curFeature);
		}
		m.setFeatureTable(featureTable);
		m.getStructure().setRoot(nroot.getStructure());

		if (m instanceof FeatureModel) {
			((FeatureModel) m).updateNextElementId();
		}

		if (considerConstraints) {
			final ArrayList<IConstraint> innerConstraintList = new ArrayList<>();
			for (final IConstraint constaint : orgFeatureModel.getConstraints()) {
				final Collection<IFeature> containedFeatures = constaint.getContainedFeatures();
				boolean containsAllfeatures = !containedFeatures.isEmpty();
				for (final IFeature feature : containedFeatures) {
					if (!selectedFeatureNames.contains(feature.getName())) {
						containsAllfeatures = false;
						break;
					}
				}
				if (containsAllfeatures) {
					innerConstraintList.add(constaint);
				}
			}
			for (final IConstraint constraint : innerConstraintList) {
				m.addConstraint(constraint.clone(m));
			}
		}
		monitor.step();

		return m;
	}

	private boolean cut(final IFeature curFeature) {
		final IFeatureStructure structure = curFeature.getStructure();
		final boolean notSelected = curFeature.getName().equals(MARK1);

		final List<IFeature> list = FeatureUtils.convertToFeatureList(structure.getChildren());
		if (list.isEmpty()) {
			return notSelected;
		} else {
			final boolean[] remove = new boolean[list.size()];
			int removeCount = 0;

			int i = 0;
			for (final IFeature child : list) {
				remove[i++] = cut(child);
			}

			// remove children
			final Iterator<IFeature> it = list.iterator();
			for (i = 0; i < remove.length; i++) {
				final IFeature feat = it.next();
				if (remove[i]) {
					it.remove();
					feat.getStructure().getParent().removeChild(feat.getStructure());
					feat.getStructure().setParent(null);
					removeCount++;
					// changed = true;
				}
			}
			if (list.isEmpty()) {
				structure.setAnd();
				return notSelected;
			} else {
				switch (getGroup(structure)) {
				case GROUP_OR:
					if (removeCount > 0) {
						structure.setAnd();
						for (final IFeature child : list) {
							child.getStructure().setMandatory(false);
						}
					} else if (list.size() == 1) {
						structure.setAnd();
						for (final IFeature child : list) {
							child.getStructure().setMandatory(true);
						}
					}
					break;
				case GROUP_ALT:
					if (removeCount > 0) {
						if (list.size() == 1) {
							structure.setAnd();
							for (final IFeature child : list) {
								child.getStructure().setMandatory(false);
							}
						} else {
							final IFeatureModel featureModel = curFeature.getFeatureModel();
							final IFeature pseudoAlternative = FMFactoryManager.getInstance().getFactory(featureModel).createFeature(featureModel, MARK2);
							pseudoAlternative.getStructure().setMandatory(false);
							pseudoAlternative.getStructure().setAlternative();
							for (final IFeature child : list) {
								structure.removeChild(child.getStructure());
								pseudoAlternative.getStructure().addChild(child.getStructure());
							}
							list.clear();
							structure.setAnd();
							structure.addChild(pseudoAlternative.getStructure());
						}
					} else if (list.size() == 1) {
						structure.setAnd();
						for (final IFeature child : list) {
							child.getStructure().setMandatory(true);
						}
					}
					break;
				}
			}
		}
		return false;
	}

	private void deleteFeature(IFeatureStructure curFeature) {
		final IFeatureStructure parent = curFeature.getParent();
		final List<IFeatureStructure> children = curFeature.getChildren();
		parent.removeChild(curFeature);
		changed = true;
		for (final IFeatureStructure child : children) {
			parent.addChild(child);
		}
		children.clear();// XXX code smell
	}

	private int getGroup(IFeatureStructure f) {
		if (f == null) {
			return GROUP_NO;
		} else if (f.isAnd()) {
			return GROUP_AND;
		} else if (f.isOr()) {
			return GROUP_OR;
		} else {
			return GROUP_ALT;
		}
	}

	private void merge(IFeatureModelFactory factory, CNF slicedFeatureModelCNF, IFeatureModel featureTree, IMonitor<?> monitor) {
		monitor.setTaskName("Adding Constraints");

		final CNF featureTreeCNF = CNFCreator.createNodes(featureTree);
		final IVariables variables = featureTreeCNF.getVariables();
		final List<LiteralSet> children = slicedFeatureModelCNF.adaptClauseList(variables);
		monitor.setRemainingWork(children.size() + 1);

		final SimpleSatSolver s = new SimpleSatSolver(featureTreeCNF);
		monitor.step();

		for (final LiteralSet clause : children) {
			switch (s.hasSolution(clause.negate())) {
			case FALSE:
				break;
			case TIMEOUT:
			case TRUE:
				featureTree.addConstraint(factory.createConstraint(featureTree, Nodes.convert(variables, clause)));
				break;
			default:
				assert false;
			}
			monitor.step();
		}
	}

	private void merge(IFeatureStructure curFeature, int parentGroup) {
		if (!curFeature.hasChildren()) {
			return;
		}
		int curFeatureGroup = getGroup(curFeature);
		final LinkedList<IFeatureStructure> list = new LinkedList<>(curFeature.getChildren());
		try {
			for (final IFeatureStructure child : list) {
				merge(child, curFeatureGroup);
				curFeatureGroup = getGroup(curFeature);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}

		if (curFeature.getFeature().getName().equals(MARK1)) {
			if (parentGroup == curFeatureGroup) {
				if ((parentGroup == GROUP_AND) && !curFeature.isMandatory()) {
					for (final IFeatureStructure feature : curFeature.getChildren()) {
						feature.setMandatory(false);
					}
				}
				deleteFeature(curFeature);
			} else {
				switch (parentGroup) {
				case GROUP_AND:
					final IFeatureStructure parent = curFeature.getParent();
					if (parent.getChildrenCount() == 1) {
						switch (curFeatureGroup) {
						case GROUP_OR:
							parent.setOr();
							break;
						case GROUP_ALT:
							parent.setAlternative();
							break;
						}
						deleteFeature(curFeature);
					}
					break;
				case GROUP_OR:
					if (curFeatureGroup == GROUP_AND) {
						boolean allOptional = true;
						for (final IFeatureStructure child : list) {
							if (child.isMandatory()) {
								allOptional = false;
								break;
							}
						}
						if (allOptional) {
							deleteFeature(curFeature);
						}
					}
					break;
				case GROUP_ALT:
					if ((curFeatureGroup == GROUP_AND) && (list.size() == 1)) {
						deleteFeature(curFeature);
					}
					break;
				}
			}
		}
	}

}
