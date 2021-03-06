package com.caddish_hedgehog.hedgecam2;

import com.caddish_hedgehog.hedgecam2.Donations;
import com.caddish_hedgehog.hedgecam2.Preview.Preview;
import com.caddish_hedgehog.hedgecam2.UI.FileListDialog;
import com.caddish_hedgehog.hedgecam2.UI.SeekBarArrayPreference;
import com.caddish_hedgehog.hedgecam2.UI.SeekBarPreference;
import com.caddish_hedgehog.hedgecam2.UI.SeekBarCheckBoxPreference;
import com.caddish_hedgehog.hedgecam2.UI.SeekBarFloatPreference;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.TwoStatePreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.TwoStatePreference;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Fragment to handle the Settings UI. Note that originally this was a
 *  PreferenceActivity rather than a PreferenceFragment which required all
 *  communication to be via the bundle (since this replaced the MainActivity,
 *  meaning we couldn't access data from that class. This no longer applies due
 *  to now using a PreferenceFragment, but I've still kept with transferring
 *  information via the bundle (for the most part, at least).
 */
public class MyPreferenceFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
	private static final String TAG = "HedgeCam/MyPreferenceFragment";

	private static final String[] mode_groups = {
		"preference_category_photo_modes",
		"preference_category_flash_modes",
		"preference_category_focus_modes",
	};
	
	private final Donations donations;
	private boolean was_donations;

	MyPreferenceFragment(Activity activity) {
		donations = new Donations(activity);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		if( MyDebug.LOG )
			Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		
		final Resources resources = getActivity().getResources();

		final SharedPreferences sharedPreferences = ((MainActivity)this.getActivity()).getSharedPrefs();
		if (sharedPreferences == null) return;

		addPreferencesFromResource(R.xml.preferences);

		final Bundle bundle = getArguments();
		final int cameraId = bundle.getInt("cameraId");
		if( MyDebug.LOG )
			Log.d(TAG, "cameraId: " + cameraId);
		final int nCameras = bundle.getInt("nCameras");
		if( MyDebug.LOG )
			Log.d(TAG, "nCameras: " + nCameras);

		final String hardware_level = bundle.getString("hardware_level");
		
		PreferenceGroup buttonsGroup = (PreferenceGroup)this.findPreference("preference_screen_ctrl_panel_buttons");
		PreferenceGroup modeGroup = (PreferenceGroup)this.findPreference("preference_screen_mode_panel_buttons");
		PreferenceGroup popupGroup = (PreferenceGroup)this.findPreference("preference_category_popup_elements");
		PreferenceGroup bugfixGroup = (PreferenceGroup)this.findPreference("preference_screen_bug_fix");

		final String [] color_effects_values = bundle.getStringArray("color_effects");
		final boolean supports_color_effects = color_effects_values != null && color_effects_values.length > 0;
		final String [] scene_modes_values = bundle.getStringArray("scene_modes");
		final boolean supports_scene_modes = scene_modes_values != null && scene_modes_values.length > 0;
		final String [] white_balances_values = bundle.getStringArray("white_balances");
		final boolean supports_white_balances = white_balances_values != null && white_balances_values.length > 0;
		if( MyDebug.LOG ) {
			Log.d(TAG, "supports_color_effects: " + supports_color_effects);
			Log.d(TAG, "supports_scene_modes: " + supports_scene_modes);
			Log.d(TAG, "supports_white_balances: " + supports_white_balances);
		}
		
		if (!supports_color_effects) {
			removePref(popupGroup, Prefs.POPUP_COLOR_EFFECT);
		}
		if (!supports_scene_modes) {
			removePref(popupGroup, Prefs.POPUP_SCENE_MODE);
		}
		if (!supports_white_balances) {
			removePref(popupGroup, Prefs.POPUP_WHITE_BALANCE);
		}
		if (!supports_white_balances && !supports_scene_modes && !supports_color_effects) {
			removePref("preference_category_popup_elements", Prefs.POPUP_EXPANDED_LISTS);
		}

		final boolean supports_auto_stabilise = bundle.getBoolean("supports_auto_stabilise");
		if( MyDebug.LOG )
			Log.d(TAG, "supports_auto_stabilise: " + supports_auto_stabilise);

		if( !supports_auto_stabilise ) {
			removePref(popupGroup, Prefs.POPUP_AUTO_STABILISE);
			removePref("preference_screen_photo_settings", Prefs.AUTO_STABILISE);
		}

		final boolean supports_face_detection = bundle.getBoolean("supports_face_detection");
		if( MyDebug.LOG )
			Log.d(TAG, "supports_face_detection: " + supports_face_detection);

		if( !supports_face_detection ) {
			removePref("preference_category_camera_controls", Prefs.FACE_DETECTION);
			removePref(buttonsGroup, Prefs.CTRL_PANEL_FACE_DETECTION);
			removePref(modeGroup, Prefs.MODE_PANEL_FACE_DETECTION);
			removePref("preference_screen_sounds", Prefs.FACE_DETECTION_SOUND);
		}

		final boolean supports_flash = bundle.getBoolean("supports_flash");
		if( MyDebug.LOG )
			Log.d(TAG, "supports_flash: " + supports_flash);

		if( !supports_flash ) {
			removePref(buttonsGroup, Prefs.CTRL_PANEL_FLASH);
			removePref(modeGroup, Prefs.MODE_PANEL_FLASH);
			removePref("preference_category_modes", "preference_category_flash_modes");
		}

		final boolean supports_focus = bundle.getBoolean("supports_focus");
		if( MyDebug.LOG )
			Log.d(TAG, "supports_focus: " + supports_focus);

		if( !supports_focus ) {
			removePref(buttonsGroup, Prefs.CTRL_PANEL_FOCUS);
			removePref(modeGroup, Prefs.MODE_PANEL_FOCUS);
			removePref(bugfixGroup, Prefs.STARTUP_FOCUS);
			removePref(bugfixGroup, Prefs.FORCE_FACE_FOCUS);
			removePref(bugfixGroup, Prefs.CENTER_FOCUS);
			removePref(bugfixGroup, Prefs.UPDATE_FOCUS_FOR_VIDEO);
			removePref("preference_category_modes", "preference_category_focus_modes");
		}

		final boolean supports_metering_area = bundle.getBoolean("supports_metering_area");
		if( MyDebug.LOG )
			Log.d(TAG, "supports_metering_area: " + supports_metering_area);

		if( !supports_metering_area ) {
			removePref(buttonsGroup, Prefs.CTRL_PANEL_EXPO_METERING_AREA);
			removePref(modeGroup, Prefs.MODE_PANEL_EXPO_METERING_AREA);
		}
		
		if ( !supports_focus && !supports_metering_area ){
			removePref("preference_screen_osd", Prefs.ALT_INDICATION);
		}

		final int [] video_widths = bundle.getIntArray("video_widths");
		final int [] video_heights = bundle.getIntArray("video_heights");

		final int resolution_width = bundle.getInt("resolution_width");
		final int resolution_height = bundle.getInt("resolution_height");
		final int [] widths = bundle.getIntArray("resolution_widths");
		final int [] heights = bundle.getIntArray("resolution_heights");
		if( widths != null && heights != null ) {
			String [] entries = new String[widths.length];
			String [] values = new String[widths.length];
			for(int i=0;i<widths.length;i++) {
				entries[i] = widths[i] + " x " + heights[i] + " " + Preview.getAspectRatioMPString(widths[i], heights[i]);
				values[i] = widths[i] + " " + heights[i];
			}
			SeekBarArrayPreference lp = (SeekBarArrayPreference)findPreference("preference_resolution");
			lp.setEntries(entries);
			lp.setEntryValues(values);
			String resolution_preference_key = Prefs.getResolutionPreferenceKey();
			String resolution_value = sharedPreferences.getString(resolution_preference_key, "");
			if( MyDebug.LOG )
				Log.d(TAG, "resolution_value: " + resolution_value);
			lp.setValue(resolution_value);
			// now set the key, so we save for the correct cameraId
			lp.setKey(resolution_preference_key);
		}
		else {
			removePref("preference_screen_photo_settings", "preference_resolution");
		}

		final int preview_width = bundle.getInt("preview_width");
		final int preview_height = bundle.getInt("preview_height");
		final int [] preview_widths = bundle.getIntArray("preview_widths");
		final int [] preview_heights = bundle.getIntArray("preview_heights");
		if( preview_widths != null && preview_heights != null ) {
			int list_length_diff = 2;

			String [] entries = new String[preview_widths.length+list_length_diff];
			String [] values = new String[preview_widths.length+list_length_diff];

			entries[0] = resources.getString(R.string.auto);
			values[0] = "auto";
			
			entries[1] = resources.getString(R.string.match_target_resolution);
			values[1] = "match_target";

			for(int i=0;i<preview_widths.length;i++) {
				entries[i+list_length_diff] = preview_widths[i] + " x " + preview_heights[i];
				values[i+list_length_diff] = preview_widths[i] + " " + preview_heights[i];
			}

			ListPreference lp = (ListPreference)findPreference("preference_preview_resolution");
			lp.setEntries(entries);
			lp.setEntryValues(values);
			String preference_key = Prefs.getPreviewResolutionPreferenceKey();
			String value = sharedPreferences.getString(preference_key, "auto");
			if( MyDebug.LOG )
				Log.d(TAG, "Preview resolution value: " + value);
			lp.setValue(value);
			// now set the key, so we save for the correct cameraId
			lp.setKey(preference_key);
		}
		else {
			removePref("preference_screen_preview", "preference_preview_resolution");
		}

		final boolean supports_raw = bundle.getBoolean("supports_raw");
		if( MyDebug.LOG )
			Log.d(TAG, "supports_raw: " + supports_raw);

		if( !supports_raw ) {
			ListPreference lp = (ListPreference)findPreference(Prefs.IMAGE_FORMAT);

			String [] all_entries = resources.getStringArray(R.array.preference_image_format_entries);
			String [] all_values = resources.getStringArray(R.array.preference_image_format_values);

			String [] entries = new String[all_entries.length-1];
			String [] values = new String[all_values.length-1];

			System.arraycopy(all_entries, 0, entries, 0, entries.length);
			System.arraycopy(all_values, 0, values, 0, values.length);

			lp.setEntries(entries);
			lp.setEntryValues(values);
		}
		else {
			Preference pref = findPreference(Prefs.IMAGE_FORMAT);
			pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					if( MyDebug.LOG )
						Log.d(TAG, "clicked raw: " + newValue);
					if( newValue.equals("jpeg_raw") ) {
						// we check done_raw_info every time, so that this works if the user selects RAW again without leaving and returning to Settings
						boolean done_raw_info = sharedPreferences.contains(Prefs.DONE_RAW_INFO);
						if( !done_raw_info ) {
							AlertDialog.Builder alertDialog = new AlertDialog.Builder(MyPreferenceFragment.this.getActivity());
							alertDialog.setTitle("RAW");
							alertDialog.setMessage(R.string.raw_info);
							alertDialog.setPositiveButton(android.R.string.ok, null);
							alertDialog.setNegativeButton(R.string.dont_show_again, new OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									if( MyDebug.LOG )
										Log.d(TAG, "user clicked dont_show_again for raw info dialog");
									SharedPreferences.Editor editor = sharedPreferences.edit();
									editor.putBoolean(Prefs.DONE_RAW_INFO, true);
									editor.apply();
								}
							});
							alertDialog.show();
						}
					}
					return true;
				}
			});
		}

		final boolean supports_dro = bundle.getBoolean("supports_dro");
		if( MyDebug.LOG )
			Log.d(TAG, "supports_dro: " + supports_dro);

		final boolean supports_hdr = bundle.getBoolean("supports_hdr");
		if( MyDebug.LOG )
			Log.d(TAG, "supports_hdr: " + supports_hdr);

		if(!supports_hdr) {
			removePref("preference_screen_photo_settings", "preference_category_hdr");
			removePref(popupGroup, Prefs.POPUP_HDR_DEGHOST);
			removePref(popupGroup, Prefs.POPUP_HDR_TONEMAPPING);
			removePref("preference_category_photo_modes", "preference_photo_mode_hdr");
		}
		if (!supports_dro) {
			if(!supports_hdr) {
				removePref(popupGroup, Prefs.POPUP_HDR_UNSHARP_MASK);
				removePref(popupGroup, Prefs.POPUP_HDR_UNSHARP_MASK_RADIUS);
				removePref(popupGroup, Prefs.POPUP_HDR_LOCAL_CONTRAST);
				removePref(popupGroup, Prefs.POPUP_HDR_N_TILES);
			}
			removePref("preference_screen_photo_settings", "preference_category_dro");
			removePref("preference_category_photo_modes", "preference_photo_mode_dro");
		}

		final boolean supports_expo_bracketing = bundle.getBoolean("supports_expo_bracketing");
		if( MyDebug.LOG )
			Log.d(TAG, "supports_expo_bracketing: " + supports_expo_bracketing);

		final int max_expo_bracketing_n_images = bundle.getInt("max_expo_bracketing_n_images");
		if( MyDebug.LOG )
			Log.d(TAG, "max_expo_bracketing_n_images: " + max_expo_bracketing_n_images);
			
		final boolean supports_focus_bracketing = bundle.getBoolean("supports_focus_bracketing");
		if( MyDebug.LOG )
			Log.d(TAG, "supports_focus_bracketing: " + supports_focus_bracketing);

		final boolean supports_fast_burst = bundle.getBoolean("supports_fast_burst");
		if( MyDebug.LOG )
			Log.d(TAG, "supports_fast_burst: " + supports_fast_burst);

		final boolean supports_nr = bundle.getBoolean("supports_nr");
		if( MyDebug.LOG )
			Log.d(TAG, "supports_nr: " + supports_nr);

		if( !supports_nr ) {
			removePref("preference_screen_photo_settings", "preference_category_nr");
			removePref("preference_category_photo_modes", "preference_photo_mode_nr");
		}

		final boolean supports_exposure_compensation = bundle.getBoolean("supports_exposure_compensation");
		final int exposure_compensation_min = bundle.getInt("exposure_compensation_min");
		final int exposure_compensation_max = bundle.getInt("exposure_compensation_max");
		if( MyDebug.LOG ) {
			Log.d(TAG, "supports_exposure_compensation: " + supports_exposure_compensation);
			Log.d(TAG, "exposure_compensation_min: " + exposure_compensation_min);
			Log.d(TAG, "exposure_compensation_max: " + exposure_compensation_max);
		}

		final String [] isos = bundle.getStringArray("isos");
		final boolean supports_iso = ( isos != null && isos.length > 0 );
		if( MyDebug.LOG )
			Log.d(TAG, "supports_iso: " + supports_iso);

		final boolean supports_iso_range = bundle.getBoolean("supports_iso_range");
		final int iso_range_min = bundle.getInt("iso_range_min");
		final int iso_range_max = bundle.getInt("iso_range_max");
		if( MyDebug.LOG ) {
			Log.d(TAG, "supports_iso_range: " + supports_iso_range);
			Log.d(TAG, "iso_range_min: " + iso_range_min);
			Log.d(TAG, "iso_range_max: " + iso_range_max);
		}
		
		if (supports_iso_range ) {
			TwoStatePreference p = (TwoStatePreference)findPreference(Prefs.RESET_MANUAL_MODE);
			if (p != null) {
				String preference_key = Prefs.RESET_MANUAL_MODE + "_" + cameraId;
				p.setKey(preference_key);
				p.setChecked(sharedPreferences.getBoolean(preference_key, false));
			}
		} else {
			removePref("preference_screen_sliders", Prefs.ISO_STEPS);
			removePref("preference_screen_sliders", Prefs.EXPOSURE_STEPS);
			removePref("preference_screen_bug_fix", Prefs.RESET_MANUAL_MODE);
		}

		if( !supports_iso && !supports_iso_range ) {
			removePref(popupGroup, Prefs.POPUP_ISO);
			removePref(buttonsGroup, Prefs.CTRL_PANEL_ISO);
			removePref(modeGroup, Prefs.MODE_PANEL_ISO);
		}

		final boolean supports_exposure_time = bundle.getBoolean("supports_exposure_time");
		final long exposure_time_min = bundle.getLong("exposure_time_min");
		final long exposure_time_max = bundle.getLong("exposure_time_max");
		if( MyDebug.LOG ) {
			Log.d(TAG, "supports_exposure_time: " + supports_exposure_time);
			Log.d(TAG, "exposure_time_min: " + exposure_time_min);
			Log.d(TAG, "exposure_time_max: " + exposure_time_max);
		}
		
		if (!supports_exposure_time) {
			removePref("preference_screen_preview", Prefs.PREVIEW_MAX_EXPO);
		}

		final boolean supports_white_balance_temperature = bundle.getBoolean("supports_white_balance_temperature");
		final int white_balance_temperature_min = bundle.getInt("white_balance_temperature_min");
		final int white_balance_temperature_max = bundle.getInt("white_balance_temperature_max");
		if( MyDebug.LOG ) {
			Log.d(TAG, "supports_white_balance_temperature: " + supports_white_balance_temperature);
			Log.d(TAG, "white_balance_temperature_min: " + white_balance_temperature_min);
			Log.d(TAG, "white_balance_temperature_max: " + white_balance_temperature_max);
		}
		if (!supports_white_balance_temperature)
			removePref("preference_screen_sliders", Prefs.WHITE_BALANCE_STEPS);

		if( !supports_expo_bracketing || max_expo_bracketing_n_images <= 3 ) {
			removePref("preference_category_expo_bracketing", Prefs.EXPO_BRACKETING_N_IMAGES);
		}

		if( !supports_expo_bracketing ) {
			removePref("preference_screen_photo_settings", "preference_category_expo_bracketing");
			removePref("preference_category_photo_modes", "preference_photo_mode_expo_bracketing");
			removePref(popupGroup, Prefs.POPUP_EXPO_BRACKETING_STOPS);
		} else {
			if (supports_iso_range) {
				TwoStatePreference p = (TwoStatePreference)findPreference(Prefs.EXPO_BRACKETING_USE_ISO);
				if (p != null) {
					String preference_key = Prefs.EXPO_BRACKETING_USE_ISO + "_" + cameraId;
					p.setKey(preference_key);
					p.setChecked(sharedPreferences.getBoolean(preference_key, true));
				}
			} else {
				removePref("preference_category_expo_bracketing", Prefs.EXPO_BRACKETING_USE_ISO);
			}

			if (supports_exposure_time) {
				removePref("preference_category_expo_bracketing", Prefs.EXPO_BRACKETING_DELAY);
			}
		}
		
		if (!supports_focus_bracketing) {
			removePref("preference_screen_photo_settings", "preference_category_focus_bracketing");
			removePref("preference_category_photo_modes", "preference_photo_mode_focus_bracketing");
		}

		if (!supports_fast_burst) {
			removePref("preference_screen_photo_settings", "preference_category_fast_burst");
			removePref("preference_category_photo_modes", "preference_photo_mode_fast_burst");
		}

		if (!supports_focus_bracketing && !supports_fast_burst) {
			removePref(popupGroup, Prefs.POPUP_PHOTOS_COUNT);
		}

		if (!supports_expo_bracketing && !supports_hdr && !supports_dro && !supports_focus_bracketing && !supports_fast_burst && !supports_nr) {
			removePref(popupGroup, Prefs.POPUP_PHOTO_MODE);
			removePref(buttonsGroup, Prefs.CTRL_PANEL_PHOTO_MODE);
			removePref(modeGroup, Prefs.MODE_PANEL_PHOTO_MODE);
			removePref("preference_category_modes", "preference_category_photo_modes");
		}

		final String [] video_quality = bundle.getStringArray("video_quality");
		final String [] video_quality_string = bundle.getStringArray("video_quality_string");
		if( video_quality != null && video_quality_string != null ) {
			String [] entries = new String[video_quality.length];
			String [] values = new String[video_quality.length];
			for(int i=0;i<video_quality.length;i++) {
				entries[i] = video_quality_string[i];
				values[i] = video_quality[i];
			}
			SeekBarArrayPreference lp = (SeekBarArrayPreference)findPreference("preference_video_quality");
			lp.setEntries(entries);
			lp.setEntryValues(values);
			String video_quality_preference_key = Prefs.getVideoQualityPreferenceKey();
			String video_quality_value = sharedPreferences.getString(video_quality_preference_key, "");
			if( MyDebug.LOG )
				Log.d(TAG, "video_quality_value: " + video_quality_value);
			lp.setValue(video_quality_value);
			// now set the key, so we save for the correct cameraId
			lp.setKey(video_quality_preference_key);
		}
		else {
			removePref("preference_screen_video_settings", "preference_video_quality");
		}
		
		if( Build.VERSION.SDK_INT < Build.VERSION_CODES.N ) {
			String [] all_entries = resources.getStringArray(R.array.preference_video_format_entries);
			String [] all_values = resources.getStringArray(R.array.preference_video_format_values);
			int length = all_entries.length-1;
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
				length--;

			String [] entries = new String[length];
			String [] values = new String[length];

			int out_i = 0;
			for(int i = 0; i < all_entries.length; i++) {
				if (all_values[i].equals("mpeg4_hevc") || (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && all_values[i].equals("webm")))
					continue;
				
				entries[out_i] = all_entries[i];
				values[out_i] = all_values[i];
				
				out_i++;
			}
			ListPreference lp = (ListPreference)findPreference(Prefs.VIDEO_FORMAT);
			lp.setEntries(entries);
			lp.setEntryValues(values);
		}
		
		final String current_video_quality = bundle.getString("current_video_quality");
		final int video_frame_width = bundle.getInt("video_frame_width");
		final int video_frame_height = bundle.getInt("video_frame_height");
		final int video_bit_rate = bundle.getInt("video_bit_rate");
		final int video_frame_rate = bundle.getInt("video_frame_rate");

		final boolean supports_force_video_4k = bundle.getBoolean("supports_force_video_4k");
		if( MyDebug.LOG )
			Log.d(TAG, "supports_force_video_4k: " + supports_force_video_4k);
		if( !supports_force_video_4k || video_quality == null || video_quality_string == null ) {
			removePref("preference_category_video_advanced", Prefs.FORCE_VIDEO_4K);
		}
		
		final boolean supports_video_stabilization = bundle.getBoolean("supports_video_stabilization");
		if( MyDebug.LOG )
			Log.d(TAG, "supports_video_stabilization: " + supports_video_stabilization);
		if( !supports_video_stabilization ) {
			removePref("preference_screen_video_settings", Prefs.VIDEO_STABILIZATION);
		}

		final boolean using_camera_2 = bundle.getBoolean("using_camera_2");

		final String noise_reduction_mode = bundle.getString("noise_reduction_mode");
		final String [] noise_reduction_modes = bundle.getStringArray("noise_reduction_modes");
		if( noise_reduction_modes != null && noise_reduction_modes.length > 1 ) {
			List<String> arr_entries = Arrays.asList(resources.getStringArray(R.array.preference_noise_reduction_entries));
			List<String> arr_values = Arrays.asList(resources.getStringArray(R.array.preference_noise_reduction_values));
			String [] entries = new String[noise_reduction_modes.length];
			String [] values = new String[noise_reduction_modes.length];
			for(int i=0; i<noise_reduction_modes.length; i++) {
				int index = arr_values.indexOf(noise_reduction_modes[i]);
				entries[i] = index == -1 ? noise_reduction_modes[i] : arr_entries.get(index);
				values[i] = noise_reduction_modes[i];
			}
			ListPreference lp = (ListPreference)findPreference(Prefs.NOISE_REDUCTION);
			lp.setEntries(entries);
			lp.setEntryValues(values);
			String preference_key = Prefs.NOISE_REDUCTION + (using_camera_2 ? "_2" : "_1") + "_" + cameraId;
			if (noise_reduction_mode != null) {
				lp.setValue(noise_reduction_mode);
			}
			lp.setKey(preference_key);
		} else {
			removePref("preference_screen_filtering", Prefs.NOISE_REDUCTION);
		}

		final String edge_mode = bundle.getString("edge_mode");
		final String [] edge_modes = bundle.getStringArray("edge_modes");
		if( edge_modes != null && edge_modes.length > 1 ) {
			List<String> arr_entries = Arrays.asList(resources.getStringArray(R.array.preference_edge_entries));
			List<String> arr_values = Arrays.asList(resources.getStringArray(R.array.preference_edge_values));
			String [] entries = new String[edge_modes.length];
			String [] values = new String[edge_modes.length];
			for(int i=0; i<edge_modes.length; i++) {
				int index = arr_values.indexOf(edge_modes[i]);
				entries[i] = index == -1 ? edge_modes[i] : arr_entries.get(index);
				values[i] = edge_modes[i];
			}
			ListPreference lp = (ListPreference)findPreference(Prefs.EDGE);
			lp.setEntries(entries);
			lp.setEntryValues(values);
			String preference_key = Prefs.EDGE + (using_camera_2 ? "_2" : "_1") + "_" + cameraId;
			if (edge_mode != null) {
				lp.setValue(edge_mode);
			}
			lp.setKey(preference_key);
		} else {
			removePref("preference_screen_filtering", Prefs.EDGE);
		}

		final String zero_shutter_delay_mode = bundle.getString("zero_shutter_delay_mode");
		final String [] zero_shutter_delay_modes = bundle.getStringArray("zero_shutter_delay_modes");
		if( zero_shutter_delay_modes != null && zero_shutter_delay_modes.length > 1 ) {
			ListPreference lp = (ListPreference)findPreference(Prefs.ZERO_SHUTTER_DELAY);
			String preference_key = Prefs.ZERO_SHUTTER_DELAY + "_" + cameraId;
			if (zero_shutter_delay_mode != null) {
				lp.setValue(zero_shutter_delay_mode);
			}
			lp.setKey(preference_key);
		} else {
			removePref("preference_screen_filtering", Prefs.ZERO_SHUTTER_DELAY);
		}

		if (
			hardware_level != null && !hardware_level.equals("legacy") &&
			noise_reduction_modes != null && noise_reduction_modes.length > 1 &&
			edge_modes != null && edge_modes.length > 1
		) {
			String preference_key = Prefs.SMART_FILTER + "_" + cameraId;
			SeekBarArrayPreference lp = (SeekBarArrayPreference)findPreference(Prefs.SMART_FILTER);
			lp.setKey(preference_key);
			lp.setValue(sharedPreferences.getString(preference_key, "0"));
		} else {
			removePref("preference_screen_filtering", Prefs.SMART_FILTER);
			removePref("preference_category_hdr", Prefs.HDR_IGNORE_SMART_FILTER);
		}

		final String optical_stabilization_mode = bundle.getString("optical_stabilization_mode");
		final String [] optical_stabilization_modes = bundle.getStringArray("optical_stabilization_modes");
		if( optical_stabilization_modes != null && optical_stabilization_modes.length > 1 ) {
			List<String> arr_entries = Arrays.asList(resources.getStringArray(R.array.preference_optical_stabilization_entries));
			List<String> arr_values = Arrays.asList(resources.getStringArray(R.array.preference_optical_stabilization_values));
			String [] entries = new String[optical_stabilization_modes.length];
			String [] values = new String[optical_stabilization_modes.length];
			for(int i=0; i<optical_stabilization_modes.length; i++) {
				int index = arr_values.indexOf(optical_stabilization_modes[i]);
				entries[i] = index == -1 ? optical_stabilization_modes[i] : arr_entries.get(index);
				values[i] = optical_stabilization_modes[i];
			}
			ListPreference lp = (ListPreference)findPreference(Prefs.OPTICAL_STABILIZATION);
			lp.setEntries(entries);
			lp.setEntryValues(values);
			String preference_key = Prefs.OPTICAL_STABILIZATION + "_" + cameraId;
			if (optical_stabilization_mode != null) {
				lp.setValue(optical_stabilization_mode);
			}
			lp.setKey(preference_key);
		} else {
			removePref("preference_screen_filtering", Prefs.OPTICAL_STABILIZATION);
			removePref("preference_category_popup_elements", Prefs.POPUP_OPTICAL_STABILIZATION);
		}

		final String hot_pixel_correction_mode = bundle.getString("hot_pixel_correction_mode");
		final String [] hot_pixel_correction_modes = bundle.getStringArray("hot_pixel_correction_modes");
		if( hot_pixel_correction_modes != null && hot_pixel_correction_modes.length > 1 ) {
			List<String> arr_entries = Arrays.asList(resources.getStringArray(R.array.preference_edge_entries));
			List<String> arr_values = Arrays.asList(resources.getStringArray(R.array.preference_edge_values));
			String [] entries = new String[hot_pixel_correction_modes.length];
			String [] values = new String[hot_pixel_correction_modes.length];
			for(int i=0; i<hot_pixel_correction_modes.length; i++) {
				int index = arr_values.indexOf(hot_pixel_correction_modes[i]);
				entries[i] = index == -1 ? hot_pixel_correction_modes[i] : arr_entries.get(index);
				values[i] = hot_pixel_correction_modes[i];
			}
			ListPreference lp = (ListPreference)findPreference(Prefs.HOT_PIXEL_CORRECTION);
			lp.setEntries(entries);
			lp.setEntryValues(values);
			String preference_key = Prefs.HOT_PIXEL_CORRECTION + "_" + cameraId;
			if (hot_pixel_correction_mode != null) {
				lp.setValue(hot_pixel_correction_mode);
			}
			lp.setKey(preference_key);
		} else {
			removePref("preference_screen_filtering", Prefs.HOT_PIXEL_CORRECTION);
		}

		final boolean can_disable_shutter_sound = bundle.getBoolean("can_disable_shutter_sound");
		if( MyDebug.LOG )
			Log.d(TAG, "can_disable_shutter_sound: " + can_disable_shutter_sound);
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR1){
			ListPreference lp = (ListPreference)findPreference(Prefs.SHUTTER_SOUND);
			lp.setEntries(R.array.preference_shutter_sound_old_entries);
			lp.setEntryValues(R.array.preference_shutter_sound_old_values);
		}
		if(!using_camera_2) {
			removePref("preference_screen_sounds", Prefs.VIDEO_SOUND);
		}
		
		final boolean has_navigation_bar;
		int id = resources.getIdentifier("config_showNavigationBar", "bool", "android");
		if (id > 0)
			has_navigation_bar = resources.getBoolean(id);
		else
			has_navigation_bar = false;

		if( Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT ) {
			// Some immersive modes require KITKAT - simpler to require Kitkat for any of the menu options
			removePref("preference_screen_gui", Prefs.IMMERSIVE_MODE);
		} else if (!has_navigation_bar) {
			ListPreference lp = (ListPreference)findPreference(Prefs.IMMERSIVE_MODE);
			lp.setEntries(R.array.preference_immersive_mode_no_navigation_bar_entries);
			lp.setEntryValues(R.array.preference_immersive_mode_no_navigation_bar_values);
		}

		if( !bundle.getBoolean("supports_lock") ) {
			removePref(buttonsGroup, Prefs.CTRL_PANEL_LOCK);
			removePref(modeGroup, Prefs.MODE_PANEL_LOCK);
		}
		if( !bundle.getBoolean("supports_switch_camera") ) {
			removePref(buttonsGroup, Prefs.CTRL_PANEL_SWITCH_CAMERA);
			removePref(modeGroup, Prefs.MODE_PANEL_SWITCH_CAMERA);
		}
		if( !bundle.getBoolean("supports_exposure_button") ) {
			removePref(buttonsGroup, Prefs.CTRL_PANEL_EXPOSURE);
			removePref(modeGroup, Prefs.MODE_PANEL_EXPOSURE);
		}

		final String [] focus_values = bundle.getStringArray("focus_values");
		boolean supports_manual_focus = false;
		boolean supports_yuv = false;
		if( using_camera_2 ) {
			supports_yuv = true;
			removePref("preference_screen_preview", "preference_category_preview_advanced");
			
			if( focus_values != null && focus_values.length > 0 ) {
				for(int i=0;i<focus_values.length;i++) {
					if (focus_values[i].equals("focus_mode_manual2")) {
						supports_manual_focus = true;
						break;
					}
				}
			}
		} else {
			if (!supports_iso) {
				removePref("preference_screen_osd", Prefs.SHOW_ISO);
			}

			removePref("preference_screen_bug_fix", Prefs.CAMERA2_FAKE_FLASH);
			removePref("preference_screen_bug_fix", Prefs.FULL_SIZE_COPY);
			removePref("preference_category_expo_bracketing", Prefs.CAMERA2_FAST_BURST);
			removePref("preference_screen_video_settings", Prefs.VIDEO_LOG_PROFILE);
		}
		
		if (supports_yuv) {
			String preference_key = Prefs.ROW_SPACE_Y + "_" + cameraId;

			SeekBarCheckBoxPreference lp = (SeekBarCheckBoxPreference)findPreference(Prefs.ROW_SPACE_Y);
			lp.setValue(sharedPreferences.getString(preference_key, "default"));
			lp.setKey(preference_key);

			preference_key = Prefs.ROW_SPACE_UV + "_" + cameraId;

			lp = (SeekBarCheckBoxPreference)findPreference(Prefs.ROW_SPACE_UV);
			lp.setValue(sharedPreferences.getString(preference_key, "default"));
			lp.setKey(preference_key);
		} else {
			removePref("preference_screen_photo_settings", Prefs.UNCOMPRESSED_PHOTO);
			removePref("preference_screen_photo_settings", Prefs.YUV_CONVERSION);
			removePref("preference_screen_calibration", Prefs.ROW_SPACE_Y);
			removePref("preference_screen_calibration", Prefs.ROW_SPACE_UV);
		}

		if (supports_manual_focus) {
			String preference_key = Prefs.MIN_FOCUS_DISTANCE + "_" + cameraId;

			SeekBarCheckBoxPreference lp = (SeekBarCheckBoxPreference)findPreference(Prefs.MIN_FOCUS_DISTANCE);
			lp.setValue(sharedPreferences.getString(preference_key, "default"));
			lp.setKey(preference_key);

			preference_key = Prefs.FOCUS_DISTANCE_CALIBRATION + "_" + cameraId;
/*			EditTextPreference etp = (EditTextPreference)findPreference(Prefs.FOCUS_DISTANCE_CALIBRATION);
			etp.setText(sharedPreferences.getString(preference_key, "0"));
			etp.setKey(preference_key);*/
			
			SeekBarFloatPreference p = (SeekBarFloatPreference)findPreference(Prefs.FOCUS_DISTANCE_CALIBRATION);
			p.setValue(Float.parseFloat(sharedPreferences.getString(preference_key, "0")));
			p.setKey(preference_key);
		} else {
			removePref("preference_screen_sliders", Prefs.FOCUS_RANGE);
			removePref("preference_screen_calibration", Prefs.MIN_FOCUS_DISTANCE);
			removePref("preference_screen_calibration", Prefs.FOCUS_DISTANCE_CALIBRATION);
			removePref("preference_screen_preview", Prefs.ZOOM_WHEN_FOCUSING);
		}

		boolean has_modes = false;
		for(String group_name : mode_groups) {
			PreferenceGroup group = (PreferenceGroup)this.findPreference(group_name);
			if (group != null) {
				for (int i = 0; i < group.getPreferenceCount(); i++) {
					has_modes = true;
					TwoStatePreference pref = (TwoStatePreference)group.getPreference(i);
					String pref_key = pref.getKey() + "_" + cameraId;
					pref.setKey(pref_key);
					pref.setChecked(sharedPreferences.getBoolean(pref_key, true));
				}
			}
		}
		if (!has_modes) {
			removePref("preference_screen_popup", "preference_category_modes");
		}

		final boolean supports_camera2 = bundle.getBoolean("supports_camera2");
		if( MyDebug.LOG )
			Log.d(TAG, "supports_camera2: " + supports_camera2);
		if( supports_camera2 ) {
			final Preference pref = findPreference(Prefs.USE_CAMERA2);
			pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference arg0) {
					if( pref.getKey().equals(Prefs.USE_CAMERA2) ) {
						if( MyDebug.LOG )
							Log.d(TAG, "user clicked camera2 API - need to restart");

						((MainActivity)getActivity()).restartActivity();
						return false;
					}
					return false;
				}
			});
		}
		else {
			removePref("preference_category_mics", Prefs.USE_CAMERA2);
		}

		{
			((ListPreference)findPreference(Prefs.GHOST_IMAGE_SOURCE)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference arg0, Object newValue) {
					if( MyDebug.LOG )
						Log.d(TAG, "clicked ghost image: " + newValue);
					if( newValue.equals("file") ) {
						MainActivity main_activity = (MainActivity) MyPreferenceFragment.this.getActivity();
						if( main_activity.getStorageUtils().isUsingSAF() ) {
							main_activity.openGhostImageChooserDialogSAF(true);
							return true;
						}
						else {
							new FileListDialog(new String[] {"jpeg", "jpg", "jpe", "png"}, new FileListDialog.Listener(){
								@Override
								public void onSelected(String file) {
									SharedPreferences.Editor editor = sharedPreferences.edit();
									editor.putString(Prefs.GHOST_IMAGE_FILE, file);
									editor.apply();
								}
								
								@Override
								public void onCancelled() {
									if (sharedPreferences.getString(Prefs.GHOST_IMAGE_FILE, "").length() == 0) {
										SharedPreferences.Editor editor = sharedPreferences.edit();
										editor.putString(Prefs.GHOST_IMAGE_SOURCE, "last_photo");
										editor.apply();
									}
								}
							}).show(getFragmentManager(), "GHOST_IMAGE_FRAGMENT");
							return true;
						}
					}
					return true;
				}
			});
		}

/*		{
			final Preference pref = findPreference("preference_online_help");
			pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference arg0) {
					if( pref.getKey().equals("preference_online_help") ) {
						if( MyDebug.LOG )
							Log.d(TAG, "user clicked online help");
						MainActivity main_activity = (MainActivity)MyPreferenceFragment.this.getActivity();
						main_activity.launchOnlineHelp();
						return false;
					}
					return false;
				}
			});
		}*/

		{
			Preference pref = findPreference(Prefs.SAVE_LOCATION);
			pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference arg0) {
					if( MyDebug.LOG )
						Log.d(TAG, "clicked save location");
					MainActivity main_activity = (MainActivity)MyPreferenceFragment.this.getActivity();
					if( main_activity.getStorageUtils().isUsingSAF() ) {
						main_activity.openFolderChooserDialogSAF(true, false);
						return true;
					}
					else {
						new FileListDialog(Prefs.SAVE_LOCATION, new FileListDialog.Listener(){
							@Override
							public void onSelected(String folder) {
								MainActivity main_activity = (MainActivity)MyPreferenceFragment.this.getActivity();
								main_activity.updateSaveFolder(folder, false);
							}
						}).show(getFragmentManager(), "FOLDER_FRAGMENT");
						return true;
					}
				}
			});
		}
		{
			((ListPreference)findPreference(Prefs.SAVE_VIDEO_FOLDER)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference arg0, Object newValue) {
					if( newValue.equals("folder") ) {
						MainActivity main_activity = (MainActivity) MyPreferenceFragment.this.getActivity();
						if( main_activity.getStorageUtils().isUsingSAF() ) {
							main_activity.openFolderChooserDialogSAF(true, true);
							return true;
						}
						else {
							new FileListDialog(Prefs.SAVE_VIDEO_LOCATION, new FileListDialog.Listener(){
								@Override
								public void onSelected(String folder) {
									MainActivity main_activity = (MainActivity)MyPreferenceFragment.this.getActivity();
									main_activity.updateSaveFolder(folder, true);
								}

								@Override
								public void onCancelled() {
									if (sharedPreferences.getString(Prefs.SAVE_VIDEO_LOCATION, "").length() == 0) {
										SharedPreferences.Editor editor = sharedPreferences.edit();
										editor.putString(Prefs.SAVE_VIDEO_FOLDER, "same_as_photo");
										editor.apply();
									}
								}
							}).show(getFragmentManager(), "FOLDER_FRAGMENT");
							return true;
						}
					}
					return true;
				}
			});
		}

		if( Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ) {
			removePref("preference_screen_files", Prefs.USING_SAF);
		}
		else {
			final Preference pref = findPreference(Prefs.USING_SAF);
			pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference arg0) {
					if( pref.getKey().equals(Prefs.USING_SAF) ) {
						if( MyDebug.LOG )
							Log.d(TAG, "user clicked saf");
						if( sharedPreferences.getBoolean(Prefs.USING_SAF, false) ) {
							if( MyDebug.LOG )
								Log.d(TAG, "saf is now enabled");
							// seems better to alway re-show the dialog when the user selects, to make it clear where files will be saved (as the SAF location in general will be different to the non-SAF one)
							//String uri = sharedPreferences.getString(Prefs.SAVE_LOCATION_SAF, "");
							//if( uri.length() == 0 )
							{
								MainActivity main_activity = (MainActivity)MyPreferenceFragment.this.getActivity();
								Toast.makeText(main_activity, R.string.saf_select_save_location, Toast.LENGTH_SHORT).show();
								main_activity.openFolderChooserDialogSAF(true, false);
							}
						}
						else {
							if( MyDebug.LOG )
								Log.d(TAG, "saf is now disabled");
						}

						if (sharedPreferences.getString(Prefs.GHOST_IMAGE_SOURCE, "last_photo").equals("file")) {
							SharedPreferences.Editor editor = sharedPreferences.edit();
							editor.putBoolean(Prefs.GHOST_IMAGE, false);
							editor.putString(Prefs.GHOST_IMAGE_SOURCE, "last_photo");
							editor.apply();
						}
					}
					return false;
				}
			});
		}

		{
			final Preference pref = findPreference("preference_about");
			pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference arg0) {
					if( pref.getKey().equals("preference_about") ) {
						if( MyDebug.LOG )
							Log.d(TAG, "user clicked about");
						AlertDialog.Builder alertDialog = new AlertDialog.Builder(MyPreferenceFragment.this.getActivity());
						alertDialog.setTitle(resources.getString(R.string.preference_about));
						final StringBuilder about_string = new StringBuilder();
						String version = "UNKNOWN_VERSION";
						int version_code = -1;
						try {
							PackageInfo pInfo = MyPreferenceFragment.this.getActivity().getPackageManager().getPackageInfo(MyPreferenceFragment.this.getActivity().getPackageName(), 0);
							version = pInfo.versionName;
							version_code = pInfo.versionCode;
						}
						catch(NameNotFoundException e) {
							if( MyDebug.LOG )
								Log.d(TAG, "NameNotFoundException exception trying to get version number");
							e.printStackTrace();
						}
						about_string.append("HedgeCam v");
						about_string.append(version);
						about_string.append("\n\n(c) 2016-2018 alex82 aka Caddish Hedgehog");
						about_string.append("\n\n");
						about_string.append(resources.getString(R.string.about_credits));
						final String translation = resources.getString(R.string.translation_author);
						if (translation.length() > 0) {
							about_string.append("\n\n");
							about_string.append(resources.getString(R.string.preference_about_translation));
							about_string.append(": ");
							about_string.append(translation);
						}
						if (was_donations) {
							about_string.append("\n\n" + resources.getString(R.string.thank_you_summary));
						}
						alertDialog.setMessage(about_string);
						alertDialog.setPositiveButton(android.R.string.ok, null);
						alertDialog.show();
						return false;
					}
					return false;
				}
			});
		}
		{
			final Preference pref = findPreference("preference_info");
			pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference arg0) {
					if( pref.getKey().equals("preference_info") ) {
						if( MyDebug.LOG )
							Log.d(TAG, "user clicked info");
						AlertDialog.Builder alertDialog = new AlertDialog.Builder(MyPreferenceFragment.this.getActivity());
						alertDialog.setTitle(resources.getString(R.string.preference_info));
						final StringBuilder about_string = new StringBuilder();
						String version = "UNKNOWN_VERSION";
						int version_code = -1;
						try {
							PackageInfo pInfo = MyPreferenceFragment.this.getActivity().getPackageManager().getPackageInfo(MyPreferenceFragment.this.getActivity().getPackageName(), 0);
							version = pInfo.versionName;
							version_code = pInfo.versionCode;
						}
						catch(NameNotFoundException e) {
							if( MyDebug.LOG )
								Log.d(TAG, "NameNotFoundException exception trying to get version number");
							e.printStackTrace();
						}
						about_string.append("HedgeCam v");
						about_string.append(version);
						about_string.append("\nPackage: ");
						about_string.append(MyPreferenceFragment.this.getActivity().getPackageName());
						about_string.append("\nVersion code: ");
						about_string.append(version_code);
						about_string.append("\nAndroid API version: ");
						about_string.append(Build.VERSION.SDK_INT);
						about_string.append("\nDevice manufacturer: ");
						about_string.append(Build.MANUFACTURER);
						about_string.append("\nDevice model: ");
						about_string.append(Build.MODEL);
						about_string.append("\nDevice code name: ");
						about_string.append(Build.DEVICE);
						about_string.append("\nDevice hardware: ");
						about_string.append(Build.HARDWARE);
						about_string.append("\nBoard name: ");
						about_string.append(Build.BOARD);
						about_string.append("\nLanguage: ");
						about_string.append(Locale.getDefault().getLanguage());
						{
							ActivityManager activityManager = (ActivityManager) getActivity().getSystemService(Activity.ACTIVITY_SERVICE);
							about_string.append("\nStandard max heap?: ");
							about_string.append(activityManager.getMemoryClass());
							about_string.append("\nLarge max heap?: ");
							about_string.append(activityManager.getLargeMemoryClass());
						}
						{
							Point display_size = new Point();
							Display display = MyPreferenceFragment.this.getActivity().getWindowManager().getDefaultDisplay();
							display.getSize(display_size);
							about_string.append("\nDisplay size: ");
							about_string.append(display_size.x);
							about_string.append("x");
							about_string.append(display_size.y);
						}
						about_string.append("\nHas navigation bar?: ");
						about_string.append(has_navigation_bar ? "yes" : "no");
						about_string.append("\nCurrent camera ID: ");
						about_string.append(cameraId);
						about_string.append("\nNo. of cameras: ");
						about_string.append(nCameras);
						about_string.append("\nCamera API: ");
						about_string.append(using_camera_2 ? "2" : "1");
						if (hardware_level != null) {
							about_string.append("\nHardware level: ");
							about_string.append(hardware_level);
						}
						{
							String last_video_error = sharedPreferences.getString("last_video_error", "");
							if( last_video_error != null && last_video_error.length() > 0 ) {
								about_string.append("\nLast video error: ");
								about_string.append(last_video_error);
							}
						}
						if( preview_widths != null && preview_heights != null ) {
							about_string.append("\nPreview resolutions: ");
							for(int i=0;i<preview_widths.length;i++) {
								if( i > 0 ) {
									about_string.append(", ");
								}
								about_string.append(preview_widths[i]);
								about_string.append("x");
								about_string.append(preview_heights[i]);
							}
						}
						about_string.append("\nPreview resolution: ");
						about_string.append(preview_width);
						about_string.append("x");
						about_string.append(preview_height);
						if( widths != null && heights != null ) {
							about_string.append("\nPhoto resolutions: ");
							for(int i=0;i<widths.length;i++) {
								if( i > 0 ) {
									about_string.append(", ");
								}
								about_string.append(widths[i]);
								about_string.append("x");
								about_string.append(heights[i]);
							}
						}
						about_string.append("\nPhoto resolution: ");
						about_string.append(resolution_width);
						about_string.append("x");
						about_string.append(resolution_height);
						if( video_quality != null ) {
							about_string.append("\nVideo qualities: ");
							for(int i=0;i<video_quality.length;i++) {
								if( i > 0 ) {
									about_string.append(", ");
								}
								about_string.append(video_quality[i]);
							}
						}
						if( video_widths != null && video_heights != null ) {
							about_string.append("\nVideo resolutions: ");
							for(int i=0;i<video_widths.length;i++) {
								if( i > 0 ) {
									about_string.append(", ");
								}
								about_string.append(video_widths[i]);
								about_string.append("x");
								about_string.append(video_heights[i]);
							}
						}
						about_string.append("\nVideo quality: ");
						about_string.append(current_video_quality);
						about_string.append("\nVideo frame width: ");
						about_string.append(video_frame_width);
						about_string.append("\nVideo frame height: ");
						about_string.append(video_frame_height);
						about_string.append("\nVideo bit rate: ");
						about_string.append(video_bit_rate);
						about_string.append("\nVideo frame rate: ");
						about_string.append(video_frame_rate);
						about_string.append("\nAuto-stabilise?: ");
						about_string.append(supports_auto_stabilise ? "available" : "not available");
						about_string.append("\nAuto-stabilise enabled?: ");
						about_string.append(sharedPreferences.getBoolean(Prefs.AUTO_STABILISE, false));
						about_string.append("\nFace detection?: ");
						about_string.append(supports_face_detection ? "available" : "not available");
						about_string.append("\nRAW?: ");
						about_string.append(supports_raw ? "available" : "not available");
						about_string.append("\nHDR?: ");
						about_string.append(supports_hdr ? "available" : "not available");
						about_string.append("\nExpo?: ");
						about_string.append(supports_expo_bracketing ? "available" : "not available");
						about_string.append("\nExpo compensation?: ");
						about_string.append(supports_exposure_compensation ? "available" : "not available");
						if( supports_exposure_compensation ) {
							about_string.append("\nExposure compensation range: ");
							about_string.append(exposure_compensation_min);
							about_string.append(" to ");
							about_string.append(exposure_compensation_max);
						}
						about_string.append("\nManual ISO?: ");
						about_string.append(supports_iso_range ? "available" : "not available");
						if( supports_iso_range ) {
							about_string.append("\nISO range: ");
							about_string.append(iso_range_min);
							about_string.append(" to ");
							about_string.append(iso_range_max);
						}
						about_string.append("\nManual exposure?: ");
						about_string.append(supports_exposure_time ? "available" : "not available");
						if( supports_exposure_time ) {
							about_string.append("\nExposure range: ");
							about_string.append(exposure_time_min);
							about_string.append(" to ");
							about_string.append(exposure_time_max);
						}
						about_string.append("\nManual WB?: ");
						about_string.append(supports_white_balance_temperature ? "available" : "not available");
						if( supports_white_balance_temperature ) {
							about_string.append("\nWB temperature: ");
							about_string.append(white_balance_temperature_min);
							about_string.append(" to ");
							about_string.append(white_balance_temperature_max);
						}
						about_string.append("\nVideo stabilization?: ");
						about_string.append(supports_video_stabilization ? "available" : "not available");
						about_string.append("\nCan disable shutter sound?: ");
						about_string.append(can_disable_shutter_sound ? "yes" : "no");
						about_string.append("\nFlash modes: ");
						String [] flash_values = bundle.getStringArray("flash_values");
						if( flash_values != null && flash_values.length > 0 ) {
							for(int i=0;i<flash_values.length;i++) {
								if( i > 0 ) {
									about_string.append(", ");
								}
								about_string.append(flash_values[i]);
							}
						}
						else {
							about_string.append("None");
						}
						about_string.append("\nFocus modes: ");
						if( focus_values != null && focus_values.length > 0 ) {
							for(int i=0;i<focus_values.length;i++) {
								if( i > 0 ) {
									about_string.append(", ");
								}
								about_string.append(focus_values[i]);
							}
						}
						else {
							about_string.append("None");
						}
						if( supports_color_effects ) {
							about_string.append("\nColor effects: ");
							for(int i=0;i<color_effects_values.length;i++) {
								if( i > 0 ) {
									about_string.append(", ");
								}
								about_string.append(color_effects_values[i]);
							}
						}
						else {
							about_string.append("None");
						}
						if( supports_scene_modes ) {
							about_string.append("\nScene modes: ");
							for(int i=0;i<scene_modes_values.length;i++) {
								if( i > 0 ) {
									about_string.append(", ");
								}
								about_string.append(scene_modes_values[i]);
							}
						}
						else {
							about_string.append("None");
						}
						if( supports_white_balances ) {
							about_string.append("\nWhite balances: ");
							for(int i=0;i<white_balances_values.length;i++) {
								if( i > 0 ) {
									about_string.append(", ");
								}
								about_string.append(white_balances_values[i]);
							}
						}
						else {
							about_string.append("None");
						}
						if( !using_camera_2 ) {
							about_string.append("\nISOs: ");
							String[] isos = bundle.getStringArray("isos");
							if (isos != null && isos.length > 0) {
								for (int i = 0; i < isos.length; i++) {
									if (i > 0) {
										about_string.append(", ");
									}
									about_string.append(isos[i]);
								}
							} else {
								about_string.append("None");
							}
							String iso_key = bundle.getString("iso_key");
							if (iso_key != null) {
								about_string.append("\nISO key: ");
								about_string.append(iso_key);
							}
						}

						if (noise_reduction_modes != null && noise_reduction_modes.length > 0) {
							about_string.append("\nNoise reduction modes: ");
							for (int i = 0; i < noise_reduction_modes.length; i++) {
								if (i > 0) {
									about_string.append(", ");
								}
								about_string.append(noise_reduction_modes[i]);
							}
							about_string.append("\nNoise reduction mode: ");
							if (noise_reduction_mode == null) about_string.append("None");
							else about_string.append(noise_reduction_mode);
						}
						if (edge_modes != null && edge_modes.length > 0) {
							about_string.append("\nEdge modes: ");
							for (int i = 0; i < edge_modes.length; i++) {
								if (i > 0) {
									about_string.append(", ");
								}
								about_string.append(edge_modes[i]);
							}
							about_string.append("\nEdge mode: ");
							if (edge_mode == null) about_string.append("None");
							else about_string.append(edge_mode);
						}

						if (optical_stabilization_modes != null && optical_stabilization_modes.length > 0) {
							about_string.append("\nOptical stabilization modes: ");
							for (int i = 0; i < optical_stabilization_modes.length; i++) {
								if (i > 0) {
									about_string.append(", ");
								}
								about_string.append(optical_stabilization_modes[i]);
							}
							about_string.append("\nOptical stabilization: ");
							if (optical_stabilization_mode == null) about_string.append("None");
							else about_string.append(optical_stabilization_mode);
						}

						if (hot_pixel_correction_modes != null && hot_pixel_correction_modes.length > 0) {
							about_string.append("\nHot pixel modes: ");
							for (int i = 0; i < hot_pixel_correction_modes.length; i++) {
								if (i > 0) {
									about_string.append(", ");
								}
								about_string.append(hot_pixel_correction_modes[i]);
							}
							about_string.append("\nHot pixel mode: ");
							if (hot_pixel_correction_mode == null) about_string.append("None");
							else about_string.append(hot_pixel_correction_mode);
						}

						about_string.append("\nUsing SAF?: ");
						about_string.append(sharedPreferences.getBoolean(Prefs.USING_SAF, false));
						String save_location = sharedPreferences.getString(Prefs.SAVE_LOCATION, "HedgeCam");
						about_string.append("\nSave Location: ");
						about_string.append(save_location);
						String save_location_saf = sharedPreferences.getString(Prefs.SAVE_LOCATION_SAF, "");
						about_string.append("\nSave Location SAF: ");
						about_string.append(save_location_saf);

						about_string.append("\nParameters: ");
						String parameters_string = bundle.getString("parameters_string");
						if( parameters_string != null ) {
							about_string.append(parameters_string);
						}
						else {
							about_string.append("None");
						}
						
						alertDialog.setMessage(about_string);
						alertDialog.setPositiveButton(android.R.string.ok, null);
						alertDialog.setNegativeButton(R.string.about_copy_to_clipboard, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								if( MyDebug.LOG )
									Log.d(TAG, "user clicked copy to clipboard");
								ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Activity.CLIPBOARD_SERVICE);
								ClipData clip = ClipData.newPlainText("About", about_string);
								clipboard.setPrimaryClip(clip);
							}
						});
						alertDialog.show();
						return false;
					}
					return false;
				}
			});
		}

		{
			final Preference pref = findPreference("preference_reset");
			pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference arg0) {
					if( pref.getKey().equals("preference_reset") ) {
						if( MyDebug.LOG )
							Log.d(TAG, "user clicked reset");
						new AlertDialog.Builder(MyPreferenceFragment.this.getActivity())
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setTitle(R.string.preference_reset)
						.setMessage(R.string.preference_reset_question)
						.setPositiveButton(R.string.answer_yes, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								if( MyDebug.LOG )
									Log.d(TAG, "user confirmed reset");
								Prefs.reset();
								MainActivity main_activity = (MainActivity)MyPreferenceFragment.this.getActivity();
								main_activity.setDeviceDefaults();
								if( MyDebug.LOG )
									Log.d(TAG, "user clicked reset - need to restart");
								
								((MainActivity)getActivity()).restartActivity();
							}
						})
						.setNegativeButton(R.string.answer_no, null)
						.show();
					}
					return false;
				}
			});
		}

		{
			final Preference pref = findPreference("preference_backup");
			pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference arg0) {
					if( pref.getKey().equals("preference_backup") ) {
						if( MyDebug.LOG )
							Log.d(TAG, "user clicked backup");
						boolean result = Prefs.backup();
						Toast.makeText((MainActivity)MyPreferenceFragment.this.getActivity(), result ? R.string.backup_saved : R.string.failed_to_save_backup, Toast.LENGTH_SHORT).show();
					}
					return false;
				}
			});
		}

		{
			final Preference pref = findPreference("preference_restore");
			pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference arg0) {
					if( pref.getKey().equals("preference_restore") ) {
						if( MyDebug.LOG )
							Log.d(TAG, "user clicked restore");

						MainActivity main_activity = (MainActivity)MyPreferenceFragment.this.getActivity();
						if( main_activity.getStorageUtils().isUsingSAF() ) {
							main_activity.openBackupChooserDialogSAF(true);
							return true;
						}
						else {
							new FileListDialog(new String[] {"xml"}, new FileListDialog.Listener(){
								@Override
								public void onSelected(String file) {
									restoreSettings(file, null);
								}
							}).show(getFragmentManager(), "RESTORE_FRAGMENT");
							return true;
						}
					}
					return false;
				}
			});
		}

		{
			was_donations = sharedPreferences.getBoolean("was_donations", false);

			if (!MyDebug.GOOGLE_PLAY) {
				
				PreferenceCategory cat = new PreferenceCategory(getActivity());
				cat.setKey("preference_category_webmoney_donations");
				cat.setTitle("WebMoney");
				((PreferenceGroup)findPreference("preference_screen_donate")).addPreference(cat);
				OnPreferenceClickListener listener = new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference pref) {
						ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Activity.CLIPBOARD_SERVICE);
						ClipData clip = ClipData.newPlainText("WebMoney wallet", pref.getSummary());
						clipboard.setPrimaryClip(clip);
						((MainActivity)getActivity()).getPreview().showToast(null, R.string.wallet_was_copied);
						return true;
					}
				};
				Preference pref = new Preference(getActivity());
				pref.setKey("wmu");
				pref.setTitle("WMU");
				pref.setSummary("Z843102502936");
				pref.setOnPreferenceClickListener(listener);
				cat.addPreference(pref);
				
				pref = new Preference(getActivity());
				pref.setKey("wmr");
				pref.setTitle("WMR");
				pref.setSummary("R420923406561");
				pref.setOnPreferenceClickListener(listener);
				cat.addPreference(pref);
			}

			donations.init(new Donations.DonationsListener() {
				@Override
				public void onReady() {
					if (!was_donations && donations.wasThereDonations()) {
						was_donations = true;
						SharedPreferences.Editor editor = sharedPreferences.edit();
						editor.putBoolean("was_donations", true);
						editor.apply();
					}
					
					List<Donations.PlayDonation> list = donations.getPlayDonations();
					if (list.size() > 0) {
						PreferenceCategory cat = new PreferenceCategory(getActivity());
						cat.setKey("preference_category_play_donations");
						cat.setTitle("Google Play");
						((PreferenceGroup)findPreference("preference_screen_donate")).addPreference(cat);
						
						String donate = resources.getString(R.string.donate);
						OnPreferenceClickListener listener = new OnPreferenceClickListener() {
							@Override
							public boolean onPreferenceClick(Preference pref) {
								donations.donate(pref.getKey());
								return true;
							}
						};
						for (Donations.PlayDonation item : list) {
							Preference pref = new Preference(getActivity());
							pref.setKey(item.id);
							pref.setTitle(donate + " " + item.amount);
							pref.setOnPreferenceClickListener(listener);
							
							cat.addPreference(pref);
						}
					} else if (MyDebug.GOOGLE_PLAY) {
						removePref("preference_category_mics", "preference_screen_donate");
					}
				}

				@Override
				public void onDonationMade(String id) {
					if (!was_donations) {
						was_donations = true;
						SharedPreferences.Editor editor = sharedPreferences.edit();
						editor.putBoolean("was_donations", true);
						editor.apply();
					}

					AlertDialog.Builder alertDialog = new AlertDialog.Builder(MyPreferenceFragment.this.getActivity());
					alertDialog.setTitle(resources.getString(R.string.thank_you));
					alertDialog.setMessage(resources.getString(R.string.thank_you_summary));
					alertDialog.setPositiveButton(android.R.string.ok, null);
					alertDialog.show();
				}
			});

		}
	}

	public void onResume() {
		super.onResume();
		// prevent fragment being transparent
		// note, setting color here only seems to affect the "main" preference fragment screen, and not sub-screens
		// note, on Galaxy Nexus Android 4.3 this sets to black rather than the dark grey that the background theme should be (and what the sub-screens use); works okay on Nexus 7 Android 5
		// we used to use a light theme for the PreferenceFragment, but mixing themes in same activity seems to cause problems (e.g., for EditTextPreference colors)
		TypedArray array = getActivity().getTheme().obtainStyledAttributes(new int[] {
				android.R.attr.colorBackground
		});
		int backgroundColor = array.getColor(0, Color.BLACK);
		/*if( MyDebug.LOG ) {
			int r = (backgroundColor >> 16) & 0xFF;
			int g = (backgroundColor >> 8) & 0xFF;
			int b = (backgroundColor >> 0) & 0xFF;
			Log.d(TAG, "backgroundColor: " + r + " , " + g + " , " + b);
		}*/
		getView().setBackgroundColor(backgroundColor);
		array.recycle();

		SharedPreferences sharedPreferences = ((MainActivity)this.getActivity()).getSharedPrefs();
		sharedPreferences.registerOnSharedPreferenceChangeListener(this);
	}

	public void onPause() {
		super.onPause();
	}

	/* So that manual changes to the checkbox/switch preferences, while the preferences are showing, show up;
	 * in particular, needed for preference_using_saf, when the user cancels the SAF dialog (see
	 * MainActivity.onActivityResult).
	 * Also programmatically sets summary (see setSummary).
	 */
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if( MyDebug.LOG )
			Log.d(TAG, "onSharedPreferenceChanged");
		Preference pref = findPreference(key);
		if( pref instanceof TwoStatePreference ) {
			TwoStatePreference twoStatePref = (TwoStatePreference)pref;
			twoStatePref.setChecked(prefs.getBoolean(key, true));
		}
		else if( pref instanceof  ListPreference ) {
			ListPreference listPref = (ListPreference)pref;
			listPref.setValue(prefs.getString(key, ""));
		}
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
		if( MyDebug.LOG )
			Log.d(TAG, "onActivityResult");
		if ( requestCode == MainActivity.SAF_CODE_OPEN_BACKUP ) {
			Uri fileUri = null;
			if( resultCode == Activity.RESULT_OK && resultData != null ) {
				fileUri = resultData.getData();
				if( MyDebug.LOG )
					Log.d(TAG, "returned single fileUri: " + fileUri);
				// persist permission just in case?
				final int takeFlags = resultData.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
				try {
					// Check for the freshest data.
					((MainActivity)getActivity()).getContentResolver().takePersistableUriPermission(fileUri, takeFlags);
				}
				catch(SecurityException e) {
					Log.e(TAG, "SecurityException failed to take permission");
					e.printStackTrace();
					((MainActivity)getActivity()).getPreview().showToast(null, R.string.failed_to_read_backup);
				}
			}
			
			if (fileUri != null)
				restoreSettings(null, fileUri);
		} else if (donations != null)
			donations.handleActivityResult(requestCode, resultCode, resultData);
	}
	
	private void removePref(final String pref_group_name, final String pref_name) {
		PreferenceGroup pg = (PreferenceGroup)this.findPreference(pref_group_name);
		removePref(pg, pref_name);
	}

	private void removePref(PreferenceGroup pg, final String pref_name) {
		if (pg != null) {
			Preference pref = findPreference(pref_name);
			if (pref != null) pg.removePreference(pref);
		}
	}
	
	@Override
	public void onDestroy() {
		if (donations != null)
			donations.onDestroy();

		super.onDestroy();
	}
	
	private void restoreSettings(final String file, final Uri uri) {
		new AlertDialog.Builder(MyPreferenceFragment.this.getActivity())
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setTitle(R.string.preference_restore)
		.setMessage(R.string.preference_restore_question)
		.setPositiveButton(R.string.answer_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if( MyDebug.LOG )
					Log.d(TAG, "user confirmed restore");
				boolean result = Prefs.restore(file, uri);
				if (result) {
					if( MyDebug.LOG )
						Log.d(TAG, "user clicked restore - need to restart");
					
					((MainActivity)getActivity()).restartActivity();
				} else
					((MainActivity)getActivity()).getPreview().showToast(null, R.string.failed_to_restore_from_backup);
			}
		})
		.setNegativeButton(R.string.answer_no, null)
		.show();
	}
}
