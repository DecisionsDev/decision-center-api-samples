package com.ibm.odm.tools;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import ilog.rules.dt.IlrDTController;
import ilog.rules.teamserver.brm.IlrBaseline;
import ilog.rules.teamserver.brm.IlrBaselineKind;
import ilog.rules.teamserver.brm.IlrBranch;
import ilog.rules.teamserver.brm.IlrBrmPackage;
import ilog.rules.teamserver.brm.IlrDecisionTable;
import ilog.rules.teamserver.brm.IlrManagedBranch;
import ilog.rules.teamserver.brm.IlrRuleProject;
import ilog.rules.teamserver.client.internal.BRMServerClient;
import ilog.rules.teamserver.model.IlrApplicationException;
import ilog.rules.teamserver.model.IlrCommitableObject;
import ilog.rules.teamserver.model.IlrConnectException;
import ilog.rules.teamserver.model.IlrElementDetails;
import ilog.rules.teamserver.model.IlrSession;
import ilog.rules.teamserver.model.IlrSessionHelper;
import ilog.rules.teamserver.model.finders.DataFinder;

public class ConfigureDT extends BRMServerClient {
	private final static String DS_ARGNAME = "-decisionService";
	private final static String BRANCH_ARGNAME = "-branch";
	private final static String GAPCHECK_ARGNAME = "-gapCheck";
	private final static String OVERLAPCHECK_ARGNAME = "-overlapCheck";
	private final static String AUTORESIZE_ARGNAME = "-autoResize";
	private final static String ORDERING_ARGNAME = "-manualOrdering";
	private final static String HIDDENCOLUMN_ARGNAME = "-unhideColumns";

	@Override
	protected void execute( String[] otherArgs) {
		String dsName = null;
		String gapCheck = null;
		String overlapCheck = null;
		String autoResize = null;
		String manualOrdering = null;
		String branch = null;
		String unhideColumns = null;
		if (getUsername() == null || getUrl() == null || otherArgs.length < 2) {
			System.out.println("Missing confiuration parameters");
			usage();
			return;
		}
		int i = 0;
		int valueIndex = -1;
		for (String param : otherArgs) {
			if (valueIndex == i) {
				i++;
				continue;
			}
			valueIndex = i + 1;
			String value = otherArgs.length <= valueIndex ? null: otherArgs[valueIndex];
			switch (param) {
			case DS_ARGNAME:
				dsName = value;
				break;
			case BRANCH_ARGNAME:
				branch = value;
				break;
			case GAPCHECK_ARGNAME:
				gapCheck = booleanValue(value);
				break;
			case OVERLAPCHECK_ARGNAME:
				overlapCheck = booleanValue(value);
			case AUTORESIZE_ARGNAME:
				autoResize = booleanValue(value);
				break;
			case ORDERING_ARGNAME:
				manualOrdering = booleanValue(value);
				break;
			case HIDDENCOLUMN_ARGNAME:
				unhideColumns = booleanValue(value);
				break;
			default:
				System.out.println(" unexpected argument " + param);
				usage();
				return;
			}
			i++;
		}
		if (dsName == null
				|| (gapCheck == null && overlapCheck == null && autoResize == null && manualOrdering == null)) {
			System.out.println("Missing mandatory arguments");
			usage();
			return;
		}
		System.out.println("Configure DT properties in decision service '" + dsName + "'");

		IlrSession dataProvider = null;
		try {
			IlrRuleProject dsProject;
			try {
				dataProvider = getSession();
				dsProject = IlrSessionHelper.getProjectNamed(dataProvider, dsName);
				if (dsProject == null) {
					System.out.println("Did not find decision service '" + dsName + "'.");
					return;
				}

				if (branch != null) {
					IlrBaseline bsln = IlrSessionHelper.getBaselineNamed(dataProvider, dsProject, branch);
					if (bsln != null && bsln.isBranch()) {
						updateBranch(bsln, gapCheck, overlapCheck, autoResize, manualOrdering, unhideColumns);
					}
				} else {
					List<IlrBaseline> baselines = IlrSessionHelper.getBaselines(dataProvider, dsProject);

					for (IlrBaseline ilrBaseline : baselines) {
						// only process branches
						if (ilrBaseline.getBaselineKind().equals(IlrBaselineKind.BRANCH_LITERAL)) {
							// if the branch is managed and complete do not modify
							if (((IlrBranch) ilrBaseline)
									.isInstanceOf(dataProvider.getBrmPackage().getManagedBranch().getName())) {
								IlrManagedBranch branchWork = (IlrManagedBranch) ilrBaseline;
								if (branchWork.getStatus().equals("Complete")) {
									continue;
								}
							}
							updateBranch(ilrBaseline, gapCheck, overlapCheck, autoResize, manualOrdering,
									unhideColumns);
						}
					}
				}
			} finally {
				closeSession( dataProvider);
			}

		} catch (IlrApplicationException e) {
			e.printStackTrace();
		} catch (IlrConnectException e) {
			e.printStackTrace();
		} finally {
		}
	}

	// update recursively the DT in this branch of the decision service and all the
	// referenced branch of sub projects
	private void updateBranch(IlrBaseline ilrBaseline, String gapCheck, String overlapCheck, String autoResize,
			String manualOrdering, String unhideColumns) throws IlrApplicationException, IlrConnectException {
		// TODO Auto-generated method stub

		IlrSession dataProvider = getSession();
		IlrBrmPackage brm = dataProvider.getBrmPackage();
		Locale locale = dataProvider.getReferenceLocale();
		DataFinder finder = DataFinder.createDataFinder(brm.getDecisionTable()).setUseDependencies(false);
		dataProvider.setWorkingBaseline(ilrBaseline);
		List<IlrElementDetails> tables = finder.findDetails(dataProvider);
		System.out.println("Updating " + tables.size() + " DT in " + ilrBaseline.getProject().getName() + " in branch "
				+ ilrBaseline.getName());
		for (IlrElementDetails table : tables) {
			int result = updateDT(dataProvider, (IlrDecisionTable) table, gapCheck, overlapCheck, autoResize,
					manualOrdering, unhideColumns, locale);
			// if updateDT returns 0 we need to refresh the session
			if (result == 0){
				dataProvider = getSession();
				dataProvider.setWorkingBaseline(ilrBaseline);		
			}
		}
		List<IlrBaseline> subBranches = IlrSessionHelper.computeAccessibleDependentBaselines(dataProvider, ilrBaseline);
		for (IlrBaseline subBranch : subBranches) {
			updateBranch(subBranch, gapCheck, overlapCheck, autoResize, manualOrdering, unhideColumns);
		}
	}

	private IlrSession getSession(){

		if (session != null){
			try {
				// check if the session is still available
				session.getWorkingBaseline() ;			
			} catch (Exception e) {
				// session is broke, need to create a new one
				try {
					session = super.connect();
				} catch (IlrConnectException e1) {
					session = null;
				}
			}
			return session;
		} else {
			try {
				session = super.connect();
			} catch (IlrConnectException e) {
				session = null;
			}
			return session;
		}

	}
	private int updateDT(IlrSession dataProvider, IlrDecisionTable table, String gapCheck, String overlapCheck,
			String autoResize, String manualOrdering, String unhideColumns, Locale locale) {
		int result = -1;
		try {
			IlrDTController cont;
			boolean update = false;
			try {
				cont = IlrSessionHelper.getDTController(dataProvider, table, locale);
				result = 1;
			} catch (Exception e) {
				System.out.println("Unable to load the decision table, retry...");
				dataProvider = getSession();
				cont = IlrSessionHelper.getDTController(dataProvider, table, locale);
				result = 0;
			}
			update = updateProperty(cont, "RowOrdering", manualOrdering);
			update |= updateProperty(cont, "UI.AutoResizeTable", autoResize);
			update |= updateProperty(cont, "Check.Gap", gapCheck);
			update |= updateProperty(cont, "Check.Overlap", overlapCheck);
			update |= "true".equalsIgnoreCase(unhideColumns);

			if (update) {
				String definition = null;
				try {
					definition = IlrSessionHelper.dtControllerToStorableString(dataProvider, cont);
				} catch (Exception e) {
					System.out.println("Unable to load the decision table definition, retry...");
					dataProvider = getSession();
					definition = IlrSessionHelper.dtControllerToStorableString(dataProvider, cont);
					result = 0;
				}
				if ("true".equalsIgnoreCase(unhideColumns)) {
					definition = definition.replaceAll(
							"<Property Name=\"Visible\"><!\\[CDATA\\[false\\]\\]></Property>",
							"<Property Name=\"Visible\"><![CDATA[true]]></Property>");
				}
				try {
					IlrSessionHelper.setDefinition(dataProvider, table, definition);
				} catch (Exception e) {
					System.out.println("Unable to set the decision table definition, retry...");
					dataProvider = getSession();
					IlrSessionHelper.setDefinition(dataProvider, table, definition);
					result = 0;
				}
				IlrCommitableObject co = new IlrCommitableObject(table);
				co.setRootDetails(table);
				try {
					dataProvider.commit(co);
					result = result == -1 ? 1 : result;
				} catch (Exception e) {
					System.out.println("Unable to save the decision table, retry...");
					dataProvider = getSession();
					dataProvider.commit(co);
					result = 0;
				}
			}
		} catch (Exception e) {
			// catch all exception from breaking the update
			e.printStackTrace();
		}

		return result;

	}

	private boolean updateProperty(IlrDTController controller, String key, String option) {
		Map<?, ?> props = controller.getDTModel().getProperties();
		if (option == null)
			return false;

		boolean modified = false;

		switch (key) {
		case "RowOrdering":
			if (props.get("RowOrdering") == null || props.get("RowOrdering").equals("Automatic")) {
				if (option.equals("true")) {
					controller.getDTModel().setProperty("RowOrdering", "Manual");
					modified = true;
				}
			} else {
				if (option.equals("false")) {
					controller.getDTModel().setProperty("RowOrdering", "Automatic");
					modified = true;
				}
			}
			break;
		case "UI.AutoResizeTable":
			// default is false
			if (props.get(key) == null ) {
					controller.getDTModel().setProperty(key, option.equals("true")?true:false);
					modified = true;
			} else	{
				if ( !(Boolean) (props.get(key)) && option.equals("true")) {
					controller.getDTModel().setProperty(key, true);
					modified = true;
				}
				if ((Boolean) (props.get(key)) && option.equals("false")) {
					controller.getDTModel().setProperty(key, false);
					modified = true;
				}
			}
			break;
		case "Check.Gap":
		case "Check.Overlap":
			// default is true
			if (props.get(key) == null || (Boolean) (props.get(key))) {
				if (option.equals("false")) {
					controller.getDTModel().setProperty(key, false);
					modified = true;
				}
			} else {
				if (option.equals("true")) {
					controller.getDTModel().setProperty(key, true);
					modified = true;
				}
			}
			break;
		}
		return modified;
	}

	private String booleanValue(String value) {

		if ( value == null ) return "false";
		return (value.toLowerCase().equals("true") || value.toLowerCase().equals("yes")) ? "true" : "false";
	}

	@Override
	protected String usageArgs() {
		String newLine = System.lineSeparator();
		return DS_ARGNAME + " <dsName> [" + OVERLAPCHECK_ARGNAME + " true|false] [" + GAPCHECK_ARGNAME
				+ " true|false] [" + ORDERING_ARGNAME + " true|false] [" + AUTORESIZE_ARGNAME + " true|false] ["
				+ HIDDENCOLUMN_ARGNAME + " true]" + newLine
				+ "\tUpdate all decision tables properties according to option selected in the decision service selected"
				+ newLine
				+ "\tIf an option is not selected the property is not modified. At least one option must be selected";
	}

	public static void main(String[] args) {
		(new ConfigureDT()).doExecute(args);
	}



}
