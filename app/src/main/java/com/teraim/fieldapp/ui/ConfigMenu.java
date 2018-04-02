package com.teraim.fieldapp.ui;

import android.accounts.Account;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.utils.BarcodeReader;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;


import java.io.File;
import java.net.URISyntaxException;

public class ConfigMenu extends PreferenceActivity {
	final SettingsFragment sf = new SettingsFragment();
	static boolean askForRestart = false;
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		askForRestart = false;
		// Display the fragment as the main content.
		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, sf)
				.commit();
		setTitle(R.string.settings);


	}

	public static class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

		private EditTextPreference serverPref;
		private ListPreference versionControlPref;
		private ListPreference syncPref;
		private EditTextPreference teamPref;
		private EditTextPreference userPref;
		private EditTextPreference appPref;

		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
			super.onActivityResult(requestCode,resultCode,data);
			Log.d("vortex", "IN ONACTIVITY RESULT ");

			Log.d("vortex", "request code " + requestCode + " result code " + resultCode);
			if (requestCode == Constants.QR_SCAN_REQUEST) {
				if (Activity.RESULT_OK == resultCode) {
					Log.d("vortex", "code img taken...scan!");
					String url = (new BarcodeReader(this.getActivity())).analyze();

					Log.d("vortex", "GOT " + (url == null ? "null" : url));


					//www.teraim.com?project=Rlotst&team=Rlo2017&name=Lotta&sync=Internet&control=major
					if (url != null) {
						Uri uri = Uri.parse(url);

						final String application = uri.getQueryParameter("application");
						final String team = uri.getQueryParameter("team");
						final String name = uri.getQueryParameter("name");
						final String sync = uri.getQueryParameter("sync");
						final String control = uri.getQueryParameter("control");
						//got null on host.
						String host = uri.getHost();
						final String server = uri.getPath();

						(new AlertDialog.Builder(this.getActivity())).setTitle("Recieved QR configuration")
								.setMessage("The following QR setting was received:" +
										Tools.printIfNotNull("\nApplication: ", application) +
										Tools.printIfNotNull("\nTeam: ", team) +
										Tools.printIfNotNull("\nName: ", name) +
										Tools.printIfNotNull("\nSync: ", sync) +
										Tools.printIfNotNull("\nVersion Control: ", control) +
										Tools.printIfNotNull("\nServer: ", server) +
										"\n********************" +
										"\nApply these changes?"

								)
								.setCancelable(false)
								.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {

									}
								})
								.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {


										if (team != null)
											 teamPref.setText(team);

										if (application != null) {
											appPref.setText(application);
										}
										if (name != null)
											userPref.setText(name);
										if (sync != null) {
											syncPref.setValue(sync);
											syncPref.setSummary(sync);
										}
										if (control != null) {
											versionControlPref.setValue(control);
											versionControlPref.setSummary(control);
										}
										if (server != null)
											serverPref.setText(server);
									}
								})
								.show();
						askForRestart();

					} else {
						new AlertDialog.Builder(this.getActivity()).setTitle("Bummer")
								.setMessage("NO QR code found in image.")
								.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {


									}
								})

								.setCancelable(false)
								.setIcon(android.R.drawable.ic_dialog_alert)
								.show();
					}
				}
			}
		}


		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			this.getPreferenceManager().setSharedPreferencesName(Constants.GLOBAL_PREFS);
			// Load the preferences from an XML resource
			addPreferencesFromResource(R.xml.myprefs);
			//Set default values for the prefs.
			//			getPreferenceScreen().getSharedPreferences()
			//			.registerOnSharedPreferenceChangeListener(this);
			this.getActivity().getSharedPreferences(Constants.GLOBAL_PREFS, Context.MODE_MULTI_PROCESS)
					.registerOnSharedPreferenceChangeListener(this);

			//Create a filter that stops users from entering disallowed characters.
			InputFilter filter = new InputFilter() {
				@Override
				public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
					boolean keepOriginal = true;
					StringBuilder sb = new StringBuilder(end - start);
					for (int i = start; i < end; i++) {
						char c = source.charAt(i);
						if (isCharAllowed(c)) // put your condition here
							sb.append(c);
						else
							keepOriginal = false;
					}
					if (keepOriginal)
						return null;
					else {
						if (source instanceof Spanned) {
							SpannableString sp = new SpannableString(sb);
							TextUtils.copySpansFrom((Spanned) source, start, sb.length(), null, sp, 0);
							return sp;
						} else {
							return sb;
						}
					}
				}

				private boolean isCharAllowed(char c) {
					return Character.isLetterOrDigit(c) || c=='.' || c=='_'||c=='/'|| c=='-';
				}
			};


			teamPref = (EditTextPreference) findPreference(PersistenceHelper.LAG_ID_KEY);
			teamPref.setSummary(teamPref.getText());

			ListPreference color = (ListPreference)findPreference(PersistenceHelper.DEVICE_COLOR_KEY_NEW);
			color.setSummary(color.getValue());

			versionControlPref = (ListPreference)findPreference(PersistenceHelper.VERSION_CONTROL);
			versionControlPref.setSummary(versionControlPref.getValue());

			syncPref = (ListPreference)findPreference(PersistenceHelper.SYNC_METHOD);
			syncPref.setSummary(syncPref.getValue());

			userPref = (EditTextPreference) findPreference(PersistenceHelper.USER_ID_KEY);
			userPref.setSummary(userPref.getText());


			serverPref = (EditTextPreference) findPreference(PersistenceHelper.SERVER_URL);
			serverPref.setSummary(serverPref.getText());
			serverPref.getEditText().setFilters(new InputFilter[] {filter});

			appPref = (EditTextPreference) findPreference(PersistenceHelper.BUNDLE_NAME);
			appPref.setSummary(appPref.getText());
			appPref.getEditText().setFilters(new InputFilter[] {filter});

			EditTextPreference backupPref = (EditTextPreference) findPreference(PersistenceHelper.BACKUP_LOCATION);
			if (backupPref.getText()==null) {
				backupPref.setText(Constants.DEFAULT_EXT_BACKUP_DIR);
			}
			backupPref.setSummary(backupPref.getText());

			ListPreference logLevels = (ListPreference)findPreference(PersistenceHelper.LOG_LEVEL);
			logLevels.setSummary(logLevels.getEntry());
			Preference button = findPreference("reset_cache");
			button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					new AlertDialog.Builder(getActivity())
							.setTitle(getResources().getString(R.string.resetCache))
							.setMessage(getResources().getString(R.string.reset_cache_warn))
							.setIcon(android.R.drawable.ic_dialog_alert)
							.setCancelable(false)
							.setPositiveButton(R.string.ok,new Dialog.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									String bundleName = getActivity().getSharedPreferences(Constants.GLOBAL_PREFS, Context.MODE_MULTI_PROCESS).getString(PersistenceHelper.BUNDLE_NAME,"");
									if (bundleName != null && !bundleName.isEmpty()) {
										Log.d("vortex","Erasing cache for "+bundleName);
										int n = Tools.eraseFolder(Constants.VORTEX_ROOT_DIR + bundleName + "/cache/");
										Toast.makeText(getActivity(),n+" "+getResources().getString(R.string.reset_cache_toast),Toast.LENGTH_LONG).show();
										askForRestart();
									}
								}

                            } )
							.setNegativeButton(R.string.cancel, new OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {

								}
							})
							.show();
					return true;
				}
			});

			final CheckBoxPreference pref = (CheckBoxPreference)findPreference("local_config");
			final PreferenceGroup devOpt = (PreferenceGroup)findPreference("developer_options");
			final Preference folderPref = findPreference(PersistenceHelper.FOLDER);
			final Preference QRPref = findPreference("scan_qr_code");

			QRPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
					File file = new File(Constants.PIC_ROOT_DIR,Constants.TEMP_BARCODE_IMG_NAME);
					Uri outputFileUri = Uri.fromFile(file);
					intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);

					startActivityForResult(intent, Constants.QR_SCAN_REQUEST);
					return true;
				}
			});
			//check if local folder exists. If not, create it.
			pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object o) {

					if (pref.isChecked()) {
						devOpt.removePreference(folderPref);
						devOpt.addPreference(serverPref);
					} else {

						devOpt.removePreference(serverPref);
						setFolderPref(folderPref);
						devOpt.addPreference(folderPref);

					}
					return true;
				}
			});

			if (!pref.isChecked()) {
				devOpt.removePreference(folderPref);
				devOpt.addPreference(serverPref);
			} else {
				devOpt.removePreference(serverPref);
				setFolderPref(folderPref);
				devOpt.addPreference(folderPref);
			}

			folderPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					setFolderPref(folderPref);
					return true;
				}
			});




		}

		private void setFolderPref(Preference folderPref) {
			//Null if serverbased preference displayed.
			if (folderPref==null)
				return;
			String bundleName = getActivity().getSharedPreferences(Constants.GLOBAL_PREFS, Context.MODE_MULTI_PROCESS).getString(PersistenceHelper.BUNDLE_NAME,"");
			if (bundleName == null || bundleName.isEmpty())
				folderPref.setSummary("Application name missing");
			else {
				String path = Constants.VORTEX_ROOT_DIR + bundleName + "/config";
				File folder = new File(path);
				StringBuilder textToDisplay = new StringBuilder("Location: " + path);
				String filesList = "<FOLDER IS EMPTY. PLEASE ADD CONFIGURATION FILES!>";
				if (!folder.exists())
					folder.mkdir();
				File[] files = folder.listFiles();
				if (files!=null && files.length>0) {
					filesList="";
					for (File f:files) {
						filesList+=f.getName()+",";
					}
					filesList = filesList.substring(0,filesList.length()-1);
				}
				textToDisplay.append("\n"+filesList);
				folderPref.setSummary(textToDisplay.toString());
				folderPref.getEditor().putString(path,"").commit();
			}
		}


		/* (non-Javadoc)
		 * @see android.app.Fragment#onPause()
		 */
		@Override
		public void onPause() {
			this.getActivity().getSharedPreferences("GlobalPrefs", Context.MODE_MULTI_PROCESS)
					.unregisterOnSharedPreferenceChangeListener(this);
			super.onPause();
		}




		/* (non-Javadoc)
		 * @see android.app.Fragment#onResume()
		 */
		@Override
		public void onResume() {
			//this.getPreferenceManager().setSharedPreferencesName(phone);
			this.getActivity().getSharedPreferences("GlobalPrefs", Context.MODE_MULTI_PROCESS)
					.registerOnSharedPreferenceChangeListener(this);
			//getPreferenceScreen().getSharedPreferences()
			//.registerOnSharedPreferenceChangeListener(this);
			super.onResume();
		}




		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {
			Preference pref = findPreference(key);

			GlobalState gs = GlobalState.getInstance();
			Account mAccount = GlobalState.getmAccount(getActivity());
			if (pref instanceof EditTextPreference) {
				EditTextPreference etp = (EditTextPreference) pref;

				if (key.equals(PersistenceHelper.BUNDLE_NAME)) {
					Log.d("vortex","changing bundle");
					setFolderPref(findPreference(PersistenceHelper.FOLDER));
					if (etp.getText().length()!=0) {
						char[] strA = etp.getText().toCharArray();
						strA[0] = Character.toUpperCase(strA[0]);
						etp.setText(new String(strA));
						askForRestart();


					}
				}
				pref.setSummary(etp.getText());

			}
			else if (pref instanceof ListPreference) {
				ListPreference letp = (ListPreference) pref;
				pref.setSummary(letp.getEntry());
				if (letp.getKey().equals(PersistenceHelper.DEVICE_COLOR_KEY_NEW)) {
					if (letp.getValue().equals("Master"))
						Log.d("nils","Changed to MASTER");

					else if (letp.getValue().equals("Client"))
						Log.d("nils","Changed to CLIENT");
					else if (letp.getValue().equals("Solo")) {
						//Turn off sync if on
						getActivity().getSharedPreferences(Constants.GLOBAL_PREFS,Context.MODE_MULTI_PROCESS).edit().putString(PersistenceHelper.SYNC_METHOD,"NONE").apply();
						Log.d("nils","Changed to SOLO");
						Log.d("vortex","sync stopped");
						ContentResolver.setSyncAutomatically(mAccount, Start.AUTHORITY, false);
					}
					if (gs != null)  {
						askForRestart();

					}


				} //change the sync state if user swapped method.
				else if (letp.getKey().equals(PersistenceHelper.SYNC_METHOD)) {

					askForRestart();

				}

				else if (letp.getKey().equals(PersistenceHelper.LOG_LEVEL)) {
					if (gs != null)  {
						askForRestart();

					}
				}

			}

			//force redraw of menuactivity.
			Intent intent = new Intent();
			intent.setAction(MenuActivity.REDRAW);
			LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
		}




		private void askForRestart() {
			askForRestart=true;
		}



	}



	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			if (askForRestart) {

				new AlertDialog.Builder(this)
						.setTitle(R.string.restart)
						.setMessage(R.string.restartMessage)
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setCancelable(false)
						.setPositiveButton(R.string.ok,new Dialog.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								Tools.restart(ConfigMenu.this);
							}

						})
						.setNegativeButton(R.string.cancel, new Dialog.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
							}
						})
						.show();


			}

			Log.d(this.getClass().getName(), "back button pressed");
		}
		return super.onKeyDown(keyCode, event);
	}

}




