package com.teraim.fieldapp.dynamic.workflow_realizations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Vibrator;
import android.text.InputFilter;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.Rule;
import com.teraim.fieldapp.dynamic.types.SpinnerDefinition;
import com.teraim.fieldapp.dynamic.types.SpinnerDefinition.SpinnerElement;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.types.Variable.DataType;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventGenerator;
import com.teraim.fieldapp.expr.SyntaxException;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.ui.MenuActivity;
import com.teraim.fieldapp.utils.CombinedRangeAndListFilter;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;

public abstract class WF_ClickableField extends WF_Not_ClickableField implements
EventGenerator {

	final LinearLayout inputContainer;

	protected Map<Variable, View> myVars = new HashMap<Variable, View>();
	private boolean autoOpenSpinner = true;
	private GlobalState gs;
	private VariableConfiguration al;
	private static boolean HIDE = false, SHOW = true;
	private Map<Variable, String[]> values = new HashMap<Variable, String[]>();

	public abstract LinearLayout getFieldLayout();

	private final SpinnerDefinition sd;

	// Special behavior: If only a single boolean, don't open up the dialog.
	// Just set the value on click.
	private boolean singleBoolean = false;

	private Drawable originalBackground;
	protected View longClickedRow;
	protected boolean iAmOpen = false;
	private Spinner firstSpinner = null;
	protected List<Rule> myRules;
	
	

	@Override
	public Set<Variable> getAssociatedVariables() {
		return myVars.keySet();
	}

	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

		// Called when the action mode is created; startActionMode() was called
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			// Inflate a menu resource providing context menu items
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.tagpopmenu, menu);

			return true;
		}

		// Called each time the action mode is shown. Always called after
		// onCreateActionMode, but
		// may be called multiple times if the mode is invalidated.
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			MenuItem x = menu.getItem(0);
			MenuItem y = menu.getItem(1);
			MenuItem z = menu.getItem(2);
			Log.d("nils", "myVars has " + myVars.size() + " elements. "
					+ myVars.toString());
			if (myVars.size() > 0) {
				z.setVisible(true);
				List<String> row = myVars.keySet().iterator().next()
						.getBackingDataSet();
				String url = al.getUrl(row);

				if (!Tools.isNetworkAvailable(gs.getContext()) || url == null
						|| url.length() == 0)
					x.setVisible(false);
				else
					x.setVisible(true);
				if (row != null && al.getVariableDescription(row) != null
						&& al.getVariableDescription(row).length() > 0)
					y.setVisible(true);
				else
					y.setVisible(false);

			} else {
				x.setVisible(false);
				y.setVisible(false);
				z.setVisible(false);
			}
			return false; // Return false if nothing is done
		}

		// Called when the user selects a contextual menu item
		@Override
		public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
			List<String> row = null;
			Iterator<Variable> it = myVars.keySet().iterator();
			if (it.hasNext())
				row = it.next().getBackingDataSet();
			switch (item.getItemId()) {
			case R.id.menu_goto:
				if (row != null) {
					String url = al.getUrl(row);
					Intent browse = new Intent(Intent.ACTION_VIEW,
							Uri.parse(url));
					browse.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					gs.getContext().startActivity(browse);
				}
				return true;
			case R.id.menu_delete:
				Iterator<Map.Entry<Variable, View>> its = myVars.entrySet()
				.iterator();
				while (its.hasNext()) {
					Map.Entry<Variable, View> pairs = (Map.Entry<Variable, View>) its
							.next();
					Variable variable = pairs.getKey();
					Log.d("vortex", "deleting variable " + variable.getId()
							+ " with value " + variable.getValue());
					DataType type = variable.getType();
					View view = pairs.getValue();
					if (type == DataType.numeric || type == DataType.decimal
							|| type == DataType.text) {
						EditText etview = (EditText) view
								.findViewById(R.id.edit);
						etview.setText("");
					} else if (type == DataType.list) {
						LinearLayout sl = (LinearLayout) view;
						Spinner sp = (Spinner) sl.findViewById(R.id.spinner);
						if (sp.getTag(R.string.u1) != null) {
							TextView descr = (TextView) sl
									.findViewById(R.id.extendedDescr);
							descr.setText("");
						}
						sp.setSelection(-1);

					} else if (type == DataType.bool) {
						RadioGroup rbg = (RadioGroup) view
								.findViewById(R.id.radioG);
						rbg.check(-1);
					} else if (type == DataType.auto_increment) {

					}

				}
				save();
				refresh();
				mode.finish(); // Action picked, so close the CAB
				return true;
			case R.id.menu_info:
				if (row != null) {
					new AlertDialog.Builder(myContext.getContext())
					.setTitle(gs.getString(R.string.description))
					.setMessage(al.getVariableDescription(row))
					.setPositiveButton(android.R.string.yes,
							new DialogInterface.OnClickListener() {
						public void onClick(
								DialogInterface dialog,
								int which) {
							mode.finish();
						}
					})
					.setIcon(android.R.drawable.ic_dialog_info).show();
				}
				return true;
			default:
				return false;
			}
		}

		// Called when the user exits the action mode
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			mActionMode = null;
			longClickedRow.setBackgroundDrawable(originalBackground);
		}
	};

	ActionMode mActionMode;

	public WF_ClickableField(final String label, final String descriptionT,
			WF_Context context, String id, View view, boolean isVisible) {
		super(id, label, descriptionT, context, view, isVisible);
		// Log.e("nils ","Creating WF_ClickableField: label: "+label+" descr: "+descriptionT+
		// " id: "+id);

		gs = GlobalState.getInstance();

		sd = gs.getSpinnerDefinitions();
		al = gs.getVariableConfiguration();
		o = gs.getLogger();



		// SpannableString content = new SpannableString(headerT);
		// content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
		inputContainer = new LinearLayout(context.getContext());
		inputContainer.setOrientation(LinearLayout.VERTICAL);
		inputContainer.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT, 1));

		// Empty all inputs and save.
		getWidget().setClickable(true);
		getWidget().setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {

				if (mActionMode != null) {
					return false;
				}
				originalBackground = v.getBackground();
				longClickedRow = v;
				v.setBackgroundColor(Color.parseColor(Constants.Color_Pressed));

				// Start the CAB using the ActionMode.Callback defined above
				mActionMode = ((Activity) myContext.getContext())
						.startActionMode(mActionModeCallback);
				WF_ClickableField.this.getWidget().setSelected(true);
				return true;

			}
		});

		getWidget().setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {
				originalBackground = v.getBackground();
				v.setBackgroundColor(Color.parseColor(Constants.Color_Pressed));
				// special case. No dialog.
				if (singleBoolean) {
					Log.d("vortex","singleboolean true..setting radio");
					View vv = myVars.values().iterator().next();
					Variable var = myVars.keySet().iterator().next();
					String value = var.getValue();
					RadioButton ja = (RadioButton) vv.findViewById(R.id.ja);
					RadioButton nej = (RadioButton) vv.findViewById(R.id.nej);
					if (value == null || var.getValue().equals("false"))
						ja.setChecked(true);
					else
						nej.setChecked(true);
					save();
					refresh();
					v.setBackgroundDrawable(originalBackground);
				} else {
					// On click, create dialog
					AlertDialog.Builder alert = new AlertDialog.Builder(v
							.getContext());
					alert.setTitle(label);
					alert.setMessage(myDescription);
					refreshInputFields();
					iAmOpen = true;

					alert.setPositiveButton(R.string.save,
							new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							iAmOpen = false;
							save();
							refresh();
							ViewGroup x = ((ViewGroup) inputContainer
									.getParent());
							if (x != null)
								x.removeView(inputContainer);
							v.setBackgroundDrawable(originalBackground);
						}
					});
					alert.setNegativeButton(R.string.cancel,
							new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							iAmOpen = false;
							ViewGroup x = ((ViewGroup) inputContainer
									.getParent());
							if (x != null)
								x.removeView(inputContainer);
							v.setBackgroundDrawable(originalBackground);
						}
					});
					if (inputContainer.getParent() != null)
						((ViewGroup) inputContainer.getParent())
						.removeView(inputContainer);
					Dialog d = alert.setView(inputContainer).create();
					d.setCancelable(false);
					// WindowManager.LayoutParams lp = new
					// WindowManager.LayoutParams();
					// lp.copyFrom(d.getWindow().getAttributes());
					// lp.height = WindowManager.LayoutParams.FILL_PARENT;
					// lp.height = 600;

					d.show();

				}
				// d.getWindow().setAttributes(lp);

			}

		});

	}
	
	// @Override
	public void addVariable(final Variable var, boolean displayOut,
			String format, boolean isVisible, boolean showHistorical) {

		String varLabel = var.getLabel();
		String varId = var.getId();
		String hist = null;
		String[] opt = null;
		String[] val = null;
		boolean spin = false;
		if (virgin) {
			virgin = false;
			if (var.getType() != null && var.getType().equals(DataType.bool))
				singleBoolean = true;
			myDescription = al.getDescription(var.getBackingDataSet());
		} else
			// cancel singleboolean if it was set.
			if (singleBoolean)
				singleBoolean = false;
		if (showHistorical)
			hist = var.getHistoricalValue();
		




		// Set an EditText view to get user input
		if (super.getKey() == null) {
			Log.d("vortex", "Setting key variable to " + varId);
			super.setKey(var);

		}

		if (var.getType() == null) {
			o.addRow("");
			o.addRedText("VARIABLE " + var.getId()
					+ " HAS NO TYPE. TYPE ASSUMED TO BE NUMERIC");
			var.setType(DataType.numeric);
		}
		/*
		 * if (initializeFromHistorical && var.getValue()==null && hist!=null) {
		 * Log
		 * .e("nils","Historical init of variable "+var.getId()+" with value "
		 * +var.getValue()); var.setValue(hist); }
		 */
		String unit = var.getPrintedUnit();
		switch (var.getType()) {
		case bool:
			// o.addRow("Adding boolean dy-variable with label "+label+", name "+varId+", type "+var.getType().name()+" and unit "+unit.name());
			View view = LayoutInflater.from(myContext.getContext()).inflate(
					R.layout.ja_nej_radiogroup, null);
			TextView header = (TextView) view.findViewById(R.id.header);

			if (hist != null && Tools.isNumeric(hist)) {
				String histTxt = (hist.equals("true") ? gs.getContext().getString(
						R.string.yes) : gs.getContext().getString(R.string.no));
				SpannableString s = new SpannableString(varLabel + " ("
						+ histTxt + ")");
				s.setSpan(new TextAppearanceSpan(gs.getContext(),
						R.style.PurpleStyle), varLabel.length() + 2,
						s.length() - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				header.setText(s);
			} else
				header.setText(varLabel);
			inputContainer.addView(view);
			myVars.put(var, view);
			break;
		case list:
			// o.addRow("Adding spinner field for dy-variable with label "+label+", name "+varId+", type "+var.getType().name()+" and unit "+unit.name());
			LinearLayout sl = (LinearLayout) LayoutInflater.from(
					myContext.getContext()).inflate(
							R.layout.edit_field_spinner, null);
			final TextView sHeader = (TextView) sl.findViewById(R.id.header);
			final TextView sDescr = (TextView) sl
					.findViewById(R.id.extendedDescr);
			final Spinner spinner = (Spinner) sl.findViewById(R.id.spinner);
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(
					myContext.getContext(),
					android.R.layout.simple_spinner_dropdown_item,
					new ArrayList<String>());
			spinner.setAdapter(adapter);
			inputContainer.addView(sl);
			Log.d("nils", "Adding spinner for label " + label);
			if (firstSpinner == null && myVars.isEmpty() && autoOpenSpinner)
				firstSpinner = spinner;

			myVars.put(var, sl);

			sHeader.setText(varLabel + (hist != null ? " (" + hist + ")" : ""));
			String listValues = al.getTable().getElement("List Values",
					var.getBackingDataSet());
			// Parse
			if (listValues.startsWith("@file")) {
				Log.d("nils", "Found complex spinner");
				if (sd == null) {
					o.addRow("");
					o.addRedText("Spinner definition file has not loaded. Spinners cannot be created!");
				} else {
					List<SpinnerElement> elems = sd.get(var.getId().toLowerCase());
					if (elems == null) {
						Log.e("nils",
								"No spinner elements for variable "
										+ var.getId());
						Log.e("nils", "backing row: " + var.getBackingDataSet());
						o.addRow("");
						o.addRedText("Complex Spinner variable "
								+ var.getId()
								+ " is not defining any elements in the configuration file (Spinners.csv). Correct file version?");

					} else {
						Log.d("nils", "Spinner variable: " + var.getId());
						int i = 0;
						opt = new String[elems.size()];
						val = new String[elems.size()];
						for (SpinnerElement se : elems) {
							Log.d("nils", "Spinner element: " + se.opt
									+ " Value: " + se.value);
							opt[i] = se.opt;
							val[i++] = se.value;
						}
						spinner.setTag(R.string.u1, var.getId());
						values.put(var, val);
						spin = true;
					}
				}
			} else {
				if (listValues.startsWith("@col")) {
					spinner.setTag("dynamic");
				} else {
					Log.d("nils", "Found static list definition..parsing");
					opt = listValues.split("\\|");
					if (opt == null || opt.length < 2) {
						o.addRow("");
						o.addRedText("Could not split List Values for variable "
								+ var.getId() + ". Did you use '|' symbol??");
					} else {

						if (opt[0].contains("=")) {
							Log.d("nils", "found static list with value pairs");
							// we have a value.
							Log.d("nils", "List found is " + listValues
									+ "...opt has " + opt.length + " elements.");
							val = new String[opt.length];
							int c = 0;
							String tmp[];
							for (String s : opt) {
								s = s.replace("{", "");
								s = s.replace("}", "");
								tmp = s.split("=");
								if (tmp == null || tmp.length != 2) {
									Log.e("nils", "found corrupt element: " + s);
									o.addRow("");
									o.addRedText("One of the elements in list "
											+ var.getId()
											+ "has a corrupt element. Comma missing?");
									val[c] = "****";
									opt[c] = "*saknar värde*";
								} else {
									val[c] = tmp[1];
									opt[c] = tmp[0];
								}
								c++;
							}
							values.put(var, val);
						} else
							values.put(var, opt);

					}
				}
			}

			if (opt != null) {
				if (hist != null) {
					try {
						int histI = findSpinnerIndexFromValue(hist, val);
						if (histI < opt.length) {
							String histT = opt[histI];

							SpannableString s = new SpannableString(varLabel
									+ " (" + histT + ")");
							s.setSpan(new TextAppearanceSpan(gs.getContext(),
									R.style.PurpleStyle),
									varLabel.length() + 2, s.length() - 1,
									Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
							sHeader.setText(s);
						}
					} catch (NumberFormatException e) {
						Log.d("vortex", "Hist spinner value is not a number: "
								+ hist);
					}
					;
				}
				spin = true;
				adapter.addAll(opt);
				Log.d("nils", "Adapter has " + adapter.getCount() + " elements");
				adapter.notifyDataSetChanged();

			} else
				Log.e("nils",
						"Couldnt add elements to spinner - opt was null in WF_ClickableField");

			spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parentView,
						View selectedItemView, int position, long id) {
					// Check if this spinner has side effects.
					if (sd != null) {
						String emsS = (String) spinner
								.getTag(R.string.u1);
						List<SpinnerElement> ems=null;
						if (emsS!=null)
							ems = sd.get(emsS);
						@SuppressWarnings("unchecked")
						List<String> curMapping = (List<String>) spinner
						.getTag(R.string.u2);
						if (ems != null) {
							SpinnerElement e = ems.get(position);
							Log.d("nils",
									"In onItemSelected. Spinner Element is "
											+ e.opt + " with variables "
											+ e.varMapping.toString());
							if (e.varMapping != null) {
								// hide the views for the last selected.
								hideOrShowViews(curMapping, HIDE);
								hideOrShowViews(e.varMapping, SHOW);
								spinner.setTag(R.string.u2, e.varMapping);
								sDescr.setText(e.descr);
								Log.d("nils", "DESCR TEXT SET TO " + e.descr);
							}
						} 
					}
				}

				private void hideOrShowViews(List<String> varIds, boolean mode) {
					Log.d("vortex", "In hideOrShowViews...");
					if (varIds == null || varIds.size() == 0)
						return;

					for (String varId : varIds) {
						Log.d("vortex", "Trying to find " + varId);
						if (varId != null) {
							for (Variable v : myVars.keySet()) {
								Log.d("vortex", "Comparing with " + v.getId());
								if (v.getId().equalsIgnoreCase(varId.trim())) {
									Log.d("vortex", "Match! " + v.getId());
									View gView = myVars.get(v);
									gView.setVisibility(mode ? View.VISIBLE
											: View.GONE);
									if (gView instanceof LinearLayout) {
										EditText et = (EditText) gView
												.findViewById(R.id.edit);
										if (et != null && mode == HIDE) {
											Log.e("nils",
													"Setting view text to empty for "
															+ v.getId());
											et.setText("");
										}
									}
								}
							}
						}
					}
				}

				@Override
				public void onNothingSelected(AdapterView<?> parentView) {

				}

			});

			break;
		case text:
			Log.d("vortex", "Adding text field for dy-variable with label "
					+ label + ", name " + varId + ", type "
					+ var.getType().name());
			View l = LayoutInflater.from(myContext.getContext()).inflate(
					R.layout.edit_field_text, null);
			header = (TextView) l.findViewById(R.id.header);

			header.setText(varLabel + " " + unit
					+ (hist != null ? " (" + hist + ")" : ""));
			inputContainer.addView(l);
			myVars.put(var, l);
			break;
		case numeric:
		case decimal:
			// o.addRow("Adding edit field for dy-variable with label "+label+", name "+varId+", type "+numType.name()+" and unit "+unit.name());
			if (var.getType() == DataType.numeric)
				l = LayoutInflater.from(myContext.getContext()).inflate(
						R.layout.edit_field_numeric, null);
			else
				l = LayoutInflater.from(myContext.getContext()).inflate(
						R.layout.edit_field_float, null);
			header = (TextView) l.findViewById(R.id.header);
			String headerTxt = varLabel
					+ ((unit != null && unit.length() > 0) ? " (" + unit + ")"
							: "");
			if (hist != null && showHistorical) {
				SpannableString s = new SpannableString(headerTxt + " (" + hist
						+ ")");
				s.setSpan(new TextAppearanceSpan(gs.getContext(),
						R.style.PurpleStyle), headerTxt.length() + 2, s
						.length() - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				header.setText(s);
			} else
				header.setText(headerTxt);

			/*
			 * String limitDesc =
			 * al.getLimitDescription(var.getBackingDataSet()); if
			 * (limitDesc!=null&&limitDesc.length()>0) { EditText etNum =
			 * (EditText)l.findViewById(R.id.edit); CombinedRangeAndListFilter
			 * filter =
			 * FilterFactory.getInstance().createLimitFilter(var,limitDesc);
			 * etNum.setFilters(new InputFilter[] {filter}); }
			 */
			// ruleExecutor.parseFormulas(al.getDynamicLimitExpression(var.getBackingDataSet()),var.getId());
			inputContainer.addView(l);
			myVars.put(var, l);
			break;
		case auto_increment:
			Log.d("vortex", "Adding AUTO_INCREMENT variable " + var.getLabel());
			l = LayoutInflater.from(myContext.getContext()).inflate(
					R.layout.edit_field_numeric, null);
			header = (TextView) l.findViewById(R.id.header);
			header.setText(varLabel);
			EditText etNum = (EditText) l.findViewById(R.id.edit);
			etNum.setFocusable(false);
			inputContainer.addView(l);
			myVars.put(var, l);
			break;
		}

		OutC w = null;
		if (displayOut) {
			LinearLayout ll = getFieldLayout();

			/*
			 * TextView o = (TextView)ll.findViewById(R.id.outputValueField);
			 * TextView u = (TextView)ll.findViewById(R.id.outputUnitField);
			 * 
			 * String value = Variable.getPrintedValue(); if (!value.isEmpty())
			 * { o.setText(varLabel+": "+value);
			 * u.setText(" ("+Variable.getPrintedUnit()+")"); }
			 */
			w = spin ? new OutSpin(ll, opt, val) : new OutC(ll, format);
			myOutputFields.put(var, w);
			outputContainer.addView(ll);
			// refreshInputFields();
			refreshOutputField(var, w);

		}
		if (!isVisible)
			myVars.get(var).setVisibility(View.GONE);
	}

	private int findSpinnerIndexFromValue(String hist, String[] val) {
		int h = Integer.parseInt(hist);
		if (val == null)
			return h;
		int i = 0;
		for (String v : val) {
			if (Tools.isNumeric(v)) {
				if (hist.equals(v))
					return i;
			}
			i++;
		}
		return h;
	}

	private void save() {
		boolean saveEvent = false;
		String newValue = null, existingValue = null;
		// for now only delytevariabler.
		Map<Variable, String> oldValue = new HashMap<Variable, String>();
		Iterator<Map.Entry<Variable, View>> it = myVars.entrySet().iterator();
		// String invalidateKeys=null;
		Context ctx = myContext.getContext();

		while (it.hasNext()) {
			Map.Entry<Variable, View> pairs = (Map.Entry<Variable, View>) it
					.next();
			Variable variable = pairs.getKey();
			existingValue = variable.getValue();
			oldValue.put(variable, existingValue);
			DataType type = variable.getType();
			View view = pairs.getValue();
			Log.d("nils", "Existing value: " + existingValue);
			if (type == DataType.bool) {
				// Get the yes radiobutton.
				RadioGroup rbg = (RadioGroup) view.findViewById(R.id.radioG);
				// If checked set value to True.
				int id = rbg.getCheckedRadioButtonId();

				if (id == R.id.nej) {
					newValue = "false";
				} else if (id == R.id.ja) {
					newValue = "true";
				} else
					newValue = null;
			} else if (type == DataType.numeric || type == DataType.text
					|| type == DataType.decimal) {
				EditText etview = (EditText) view.findViewById(R.id.edit);
				String txt = etview.getText().toString();
				if (txt.trim().length() > 0)
					newValue = txt;
				else
					newValue = null;
			} else if (type == DataType.list) {
				LinearLayout sl = (LinearLayout) view;
				Spinner sp = (Spinner) sl.findViewById(R.id.spinner);
				int s = sp.getSelectedItemPosition();
				String v[] = values.get(variable);
				if (v != null) {
					if (s >= 0 && s < v.length)
						newValue = v[s];
					else
						newValue = null;
					Log.d("nils", "VALUE FOR SPINNER A " + newValue);
				} else {
					newValue = (String) sp.getSelectedItem();
					Log.d("nils", "VALUE FOR SPINNER B " + newValue);
				}
			} else if (type == DataType.auto_increment) {
				EditText etview = (EditText) view.findViewById(R.id.edit);
				String s = etview.getText().toString();
				if (s != null && s.length() > 0) {
					int val = Integer.parseInt(etview.getText().toString());
					val++;
					newValue = val + "";
				} else {
					Log.e("vortex", "value is null or len 0 in auto_increment");
					newValue = existingValue;
				}
			}

			if (newValue == null || !newValue.equals(existingValue)
					|| variable.isUsingDefault()) {
				Log.d("nils", "New value: " + newValue);
				saveEvent = true;

				if (newValue == null) {
					Log.e("vortex", "Calling delete on " + variable.getId()
							+ "Obj:" + variable + " with keychain\n"
							+ variable.getKeyChain().toString());
					variable.deleteValue();
					Log.e("vortex",
							"Getvalue now returns: " + variable.getValue());
				} else {
					// Re-evaluate rules.
					if (variable.hasValueOutOfRange()) {
						saveEvent = false;
						String earlierValue = variable.getValue();
						if (earlierValue == null)
							earlierValue = "";
						Vibrator myVibrator = (Vibrator) ctx
								.getSystemService(Context.VIBRATOR_SERVICE);
						myVibrator.vibrate(250);
						new AlertDialog.Builder(ctx)
						.setTitle("Incorrect value")
						.setMessage(
								"The value you entered is outside the allowed range. Earlier value will be used: ["
										+ earlierValue + "]")
										.setIcon(android.R.drawable.ic_dialog_alert)
										.setCancelable(false)
										.setNeutralButton("Ok",
												new Dialog.OnClickListener() {
											@Override
											public void onClick(
													DialogInterface dialog,
													int which) {
												// TODO Auto-generated method
												// stub

											}
										}).show();
					} else {
						// check rules if value is in range.
						variable.setOnlyCached(newValue);

					}

				}

			}
		}

		Rule r = checkRules();
		if (r != null) {
			revertChanges();
			saveEvent = false;
			Vibrator myVibrator = (Vibrator) ctx
					.getSystemService(Context.VIBRATOR_SERVICE);
			myVibrator.vibrate(250);
			new AlertDialog.Builder(ctx).setTitle(r.getRuleHeader())
			.setMessage(r.getRuleText())
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setCancelable(false)
			.setNeutralButton("Ok", new Dialog.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub

				}
			}).show();
		}
		if (saveEvent) {

			boolean contextChanged = false;
			// Commit cached value into db.
			for (Variable v : myVars.keySet()) {
				String value = v.getValue();
				v.setOnlyCached(null);
				// If value of variable has changed and it is a context
				// variable, the whole workflow needs reload!
				if (v.setValue(value) && myContext.isContextVariable(v.getId())) {
					Log.e("vortex",
							"detected change of context variable in wf_clickfield: "
									+ v.getLabel());
					contextChanged = true;
					// If context changed, rerun the workflow.

				}

			}
			if (contextChanged) {
				// No save event...reload the whole workflow instead.
				myContext.reload();
				return;
			}
			Log.d("nils", "IN SAVE() SENDING EVENT");
			gs.sendEvent(MenuActivity.REDRAW);
			myContext
			.registerEvent(new WF_Event_OnSave(this.getId(), oldValue));

			// myContext.registerEvent(new WF_Event_OnContextChange());
			// if (contextChanged)
			// myContext.reload();
		}

	}

	/**
	 * If a rule is broken, revert value changes.
	 */
	private void revertChanges() {
		for (Variable v : myVars.keySet()) {
			v.revert();
		}
	}

	private Rule checkRules() {

		if (myRules == null)
			return null;
		Log.d("vortex", "In checkRules. I have " + myRules.size() + " rules");
		for (Rule r : myRules) {
			Log.d("vortex", " Rule: " + r.getCondition());
			try {
				Boolean res = r.execute();
				if (res != null && !res)
					return r;
			} catch (SyntaxException e) {
				Log.e("vortex", "Syntax exception on rule " + r.id);

			}
		}
		return null;
	}

	// @Override
	public void refreshInputFields() {
		DataType numType;
		Log.d("nils", "In refreshinputfields");
		Set<Entry<Variable, View>> vars = myVars.entrySet();
		for (Entry<Variable, View> entry : vars) {
			Variable variable = entry.getKey();
			String value = variable.getValue();
			Log.d("nils", "Variable: " + variable.getLabel() + " value: "
					+ variable.getValue());
			numType = variable.getType();

			View v = entry.getValue();

			if (numType == DataType.bool) {
				RadioButton ja = (RadioButton) v.findViewById(R.id.ja);
				RadioButton nej = (RadioButton) v.findViewById(R.id.nej);
				if (value != null) {
					if (value.equals("true"))
						ja.setChecked(true);
					else
						nej.setChecked(true);
				}
			} else if (numType == DataType.numeric || numType == DataType.text) {

				// Log.d("nils","refreshing edittext with varid "+variable.getId());
				EditText et = (EditText) v.findViewById(R.id.edit);
				CombinedRangeAndListFilter filter = variable.getLimitFilter();
				if (filter != null)
					et.setFilters(new InputFilter[] { filter });

				TextView limit = (TextView) v.findViewById(R.id.limit);
				CharSequence limiTxt = new SpannableString("");
				et.setTextColor(Color.BLACK);
				if (variable.isUsingDefault()) {
					et.setTextColor(myContext.getContext().getResources()
							.getColor(R.color.purple));
				} else
					Log.d("nils", "Variable " + variable.getId()
							+ " is NOT YELLOW");
				if (filter != null) {
					if (variable.hasValueOutOfRange())
						et.setTextColor(Color.RED);
					limiTxt = TextUtils.concat(limiTxt, filter.prettyPrint());
				}
				et.setTextColor(Color.BLACK);
				/*
				 * CharSequence ruleExec =
				 * ruleExecutor.getRuleExecutionAsString(
				 * variable.getRuleState()); if (ruleExec!=null) { limiTxt =
				 * TextUtils.concat(limiTxt,ruleExec); if
				 * (variable.hasBrokenRules()) et.setTextColor(Color.RED); }
				 */
				limit.setText(limiTxt);
				et.setText(value == null ? "" : value);
				int position = et.getText().length();
				Selection.setSelection(et.getEditableText(), position);

			} else if (numType == DataType.list) {
				// this is the spinner.
				final Spinner sp = (Spinner) v.findViewById(R.id.spinner);

				final Handler h = new Handler();
				if (firstSpinner != null)
					new Thread(new Runnable() {
						public void run() {
							// DO NOT ATTEMPT TO DIRECTLY UPDATE THE UI HERE, IT
							// WON'T WORK!
							// YOU MUST POST THE WORK TO THE UI THREAD'S HANDLER
							h.postDelayed(new Runnable() {
								public void run() {
									// Open the Spinner...
									if (firstSpinner.isShown())
										firstSpinner.performClick();
								}
							}, 500);
						}
					}).start();

				String[] opt = null;
				String tag = (String) sp.getTag();

				String val[] = values.get(variable);
				if (val != null) {

					for (int i = 0; i < val.length; i++) {
						if (val[i].equals(variable.getValue()))
							sp.setSelection(i);
					}
				}

				else if (tag != null && tag.equals("dynamic")) {
					// Get the list values
					opt = Tools.generateList(variable);

					// Add dropdown.
					if (opt == null)
						Log.e("nils", "OPT IS STILL NULL!!!");
					else {

						((ArrayAdapter<String>) sp.getAdapter()).clear();
						((ArrayAdapter<String>) sp.getAdapter()).addAll(opt);
						String item = null;
						if (sp.getAdapter().getCount() > 0) {
							if (value != null) {
								for (int i = 0; i < sp.getAdapter().getCount(); i++) {
									item = (String) sp.getAdapter().getItem(i);
									if (item == null)
										continue;
									else if (item.equals(value))
										sp.setSelection(i);
								}
							}
						} else {
							o.addRow("");
							o.addRedText("Empty spinner for variable " + v
									+ ". Check your variable configuration.");
						}
					}
				}

			} else if (numType == DataType.auto_increment) {
				EditText et = (EditText) v.findViewById(R.id.edit);
				et.setText(value == null ? "0" : value);
			}

		}
	}

	/*
	 * private CombinedRangeAndListFilter getFilter(EditText et) { InputFilter[]
	 * tmp = et.getFilters(); return
	 * tmp.length==0?null:(CombinedRangeAndListFilter)tmp[0]; }
	 */

	protected void setAutoOpenSpinner(boolean open) {
		autoOpenSpinner = open;
	}

}
