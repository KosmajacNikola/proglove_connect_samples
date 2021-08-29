package com.example.pgsdksamplejavaapp;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import de.proglove.sdk.*;
import de.proglove.sdk.button.BlockPgTriggersParams;
import de.proglove.sdk.button.ButtonPress;
import de.proglove.sdk.button.IBlockPgTriggersCallback;
import de.proglove.sdk.button.IButtonOutput;
import de.proglove.sdk.button.IPgTriggersUnblockedOutput;
import de.proglove.sdk.button.PredefinedPgTrigger;
import de.proglove.sdk.commands.PgCommand;
import de.proglove.sdk.commands.PgCommandParams;
import de.proglove.sdk.configuration.IPgConfigProfileCallback;
import de.proglove.sdk.configuration.IPgGetConfigProfilesCallback;
import de.proglove.sdk.configuration.PgConfigProfile;
import de.proglove.sdk.display.*;
import de.proglove.sdk.scanner.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SdkActivity extends AppCompatActivity implements IServiceOutput, IScannerOutput, IButtonOutput,
        IPgTriggersUnblockedOutput, IDisplayOutput {

    private static final String TAG = SdkActivity.class.getSimpleName();
    private static int DEFAULT_IMAGE_TIMEOUT = 10000;

    private final Logger logger = Logger.getLogger(TAG);
    private final IPgManager pgManager = new PgManager(logger, Executors.newCachedThreadPool());

    private int defaultImageQuality;

    // Connection
    private Button serviceConnectBtn;
    private Button scannerConnectBtn;
    private Button scannerConnectPinnedBtn;

    // Scanning result
    private TextView scannerResultTV;
    private TextView resultSymbologyTV;

    // Feedback
    private Button triggerFeedbackBtn;
    private RadioGroup feedbackRadioGroup;
    private Switch defaultFeedbackSwitch;

    // Profiles
    private Button refreshConfigProfilesBtn;
    private TextView changeProfileLabel;
    private RecyclerView profilesRecycler;
    private ProfilesAdapter profilesAdapter;

    // Blocking trigger
    private Button blockTriggerBtn;
    private Button blockAllTriggersBtn;
    private Button unblockTriggerBtn;

    // Taking image
    private Button takeImageButtonBtn;
    private RadioGroup resolutionRadioGroup;
    private EditText imageQualityET;
    private EditText timeoutET;
    private ImageView imageTakenIV;

    // Display
    private TextView displayStateTV;
    private Button disconnectDisplayBtn;
    private Button sendTestScreenBtn, sendAnotherTestScreenBtn, sendTestScreenFailBtn, sendPG1TestScreen, sendPG1ATestScreen, pickDisplayOrientationDialogBtn;

    // CommandParams
    private Switch sendFeedbackWithReplaceQueueSwitch;

    // Device Visibility
    private Button deviceVisibilityBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sdk_sample);

        initViews();
        initClickListeners();

        setupProfilesRecycler();

        updateButtonStates();
        setDefaultImageConfiguration();

        pgManager.subscribeToServiceEvents(this);
        pgManager.subscribeToScans(this);
        pgManager.subscribeToButtonPresses(this);
        pgManager.subscribeToPgTriggersUnblocked(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Make sure that app is connected to the service to be able to use other SDK functions
        pgManager.ensureConnectionToService(getApplicationContext());
    }

    @Override
    protected void onDestroy() {
        pgManager.unsubscribeFromServiceEvents(this);
        pgManager.unsubscribeFromScans(this);
        pgManager.unsubscribeFromButtonPresses(this);
        pgManager.unsubscribeFromPgTriggersUnblocked(this);
        super.onDestroy();
    }

    /*
     * IServiceOutput Implementation:
     */
    @Override
    public void onServiceConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateButtonStates();
            }
        });
    }

    @Override
    public void onServiceDisconnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateButtonStates();
            }
        });
    }
    /*
     * End of IServiceOutput Implementation
     */

    /*
     * IScannerOutput Implementation:
     */
    @Override
    public void onBarcodeScanned(@NonNull final BarcodeScanResults barcodeScanResults) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateScannedResults(barcodeScanResults);
            }
        });
    }

    @Override
    public void onScannerConnected() {
        // Buttons already updated in #onScannerStateChanged
    }

    @Override
    public void onScannerDisconnected() {
        // Buttons already updated in #onScannerStateChanged
    }

    @Override
    public void onScannerStateChanged(@NonNull ConnectionStatus connectionStatus) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateButtonStates();
            }
        });
    }
    /*
     * End of IScannerOutput Implementation
     */

    /*
     * IButtonOutput Implementation:
     */
    @Override
    public void onButtonPressed(@NonNull final ButtonPress buttonPress) {
        String msg = getString(R.string.button_pressed, buttonPress.getId());
        showMessage(msg, false);
    }
    /*
     * End of IButtonOutput Implementation
     */

    /*
     * IDisplayOutput Implementation:
     */
    @Override
    public void onDisplayConnected() {
        // Buttons already updated in #onDisplayStateChanged
    }

    @Override
    public void onDisplayDisconnected() {
        // Buttons already updated in #onDisplayStateChanged
    }

    @Override
    public void onDisplayStateChanged(@NonNull ConnectionStatus connectionStatus) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateButtonStates();
            }
        });
    }
    /*
     * End of IDisplayOutput Implementation
     */

    /*
     * IPgTriggersUnblockedOutput Implementation:
     */

    @Override
    public void onPgTriggersUnblocked() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(
                        SdkActivity.this.getApplicationContext(),
                        "Triggers unblocked",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }
    /*
     * End of IPgTriggersUnblockedOutput Implementation
     */

    private void initViews() {
        serviceConnectBtn = findViewById(R.id.serviceConnectBtn);
        scannerConnectBtn = findViewById(R.id.connectScannerRegularBtn);
        scannerConnectPinnedBtn = findViewById(R.id.connectScannerPinnedBtn);
        scannerResultTV = findViewById(R.id.inputField);
        resultSymbologyTV = findViewById(R.id.symbologyResult);
        triggerFeedbackBtn = findViewById(R.id.triggerFeedbackButton);
        feedbackRadioGroup = findViewById(R.id.radioGroup);
        feedbackRadioGroup.check(R.id.feedbackSuccess);
        defaultFeedbackSwitch = findViewById(R.id.defaultFeedbackSwitch);
        takeImageButtonBtn = findViewById(R.id.takeImageButton);
        resolutionRadioGroup = findViewById(R.id.resolutionRadioGroup);
        imageQualityET = findViewById(R.id.jpegQualityEditText);
        timeoutET = findViewById(R.id.timeoutEditText);
        imageTakenIV = findViewById(R.id.imageTaken);
        displayStateTV = findViewById(R.id.displayStateOutput);
        disconnectDisplayBtn = findViewById(R.id.disconnectDisplayBtn);
        sendTestScreenBtn = findViewById(R.id.sendTestScreenBtn);
        sendAnotherTestScreenBtn = findViewById(R.id.sendTestScreenBtn2);
        sendPG1TestScreen = findViewById(R.id.sendPg1TestScreenBtn);
        sendPG1ATestScreen = findViewById(R.id.sendPg1ATestScreenBtn);
        sendTestScreenFailBtn = findViewById(R.id.sendTestScreenD3BtnFailing);
        pickDisplayOrientationDialogBtn = findViewById(R.id.pickDisplayOrientationDialogBtn);
        sendFeedbackWithReplaceQueueSwitch = findViewById(R.id.sendFeedbackWithReplaceQueueSwitch);
        refreshConfigProfilesBtn = findViewById(R.id.refreshConfigProfilesButton);
        changeProfileLabel = findViewById(R.id.changeProfileLabel);
        profilesRecycler = findViewById(R.id.profilesRecycler);
        blockTriggerBtn = findViewById(R.id.blockTriggerButton);
        blockAllTriggersBtn = findViewById(R.id.blockAllTriggersButton);
        unblockTriggerBtn = findViewById(R.id.unblockTriggerButton);
        deviceVisibilityBtn = findViewById(R.id.deviceVisibilityBtn);
    }

    private void initClickListeners() {
        // Connect to the PG Service
        serviceConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pgManager.ensureConnectionToService(getApplicationContext());
            }
        });

        // Pair scanner
        scannerConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onScannerConnectBtnClick(false);
            }
        });

        // Pair scanner from pinned Activity
        scannerConnectPinnedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onScannerConnectBtnClick(true);
            }
        });

        // Trigger feedback
        triggerFeedbackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerFeedback();
            }
        });

        // Changing scanner configuration
        defaultFeedbackSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                if (pgManager.isConnectedToService() && pgManager.isConnectedToScanner()) {
                    changeScannerConfig(isChecked);
                } else {
                    String msg = getString(R.string.pair_scanner_first);
                    showMessage(msg, false);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            defaultFeedbackSwitch.setChecked(true);
                        }
                    });
                }
            }
        });

        // Getting the configuration profiles
        refreshConfigProfilesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getConfigProfiles();
            }
        });

        // Blocking trigger
        blockTriggerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                blockTrigger();
            }
        });

        // Blocking all triggers for 10 seconds
        blockAllTriggersBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                blockAllTriggersFor10s();
            }
        });

        // Unblocking trigger
        unblockTriggerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unblockTrigger();
            }
        });

        // Take an image
        takeImageButtonBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTakingImage();
            }
        });

        // Disconnecting the display
        disconnectDisplayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pgManager.isConnectedToService() && pgManager.isConnectedToDisplay()) {
                    pgManager.disconnectDisplay();
                } else {
                    String msg = getString(R.string.no_display_connected);
                    showMessage(msg, false);
                }
            }
        });

        // Changing display screen
        sendTestScreenBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PgTemplateField[] data = {
                        new PgTemplateField(1, "Bezeichnung", "Kopfairbag"),
                        new PgTemplateField(2, "Fahrzeug-Typ", "Hatchback"),
                        new PgTemplateField(3, "Teilenummer", "K867 86 027 H3")
                };
                PgScreenData screenData = new PgScreenData("PG3", data, RefreshType.DEFAULT);
                sendScreen(screenData);
            }
        });

        sendAnotherTestScreenBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PgTemplateField[] data = {
                        new PgTemplateField(1, "Bezeichnung", "Gemüsemischung"),
                        new PgTemplateField(2, "Bezeichnung", "Früchte Müsli")
                };
                PgScreenData screenData = new PgScreenData("PG2", data, RefreshType.PARTIAL_REFRESH);
                sendScreen(screenData);
            }
        });

        sendPG1TestScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PgTemplateField[] data = {
                        new PgTemplateField(1, "LOGIN", "Scan to login and select a process")
                };
                PgScreenData screenData = new PgScreenData("PG1", data, RefreshType.DEFAULT);
                sendScreen(screenData);
            }
        });

        sendPG1ATestScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PgTemplateField[] data = {
                        new PgTemplateField(1, "", "Scan order to begin")
                };
                PgScreenData screenData = new PgScreenData("PG1A", data, RefreshType.DEFAULT);
                sendScreen(screenData);
            }
        });

        sendTestScreenFailBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PgTemplateField[] data = {
                        new PgTemplateField(1, "now this is the story", "all about how"),
                        new PgTemplateField(2, "my life got flipped", "turned upside down"),
                        new PgTemplateField(3, "and I'd like to take", "a minute just sit right there"),
                        new PgTemplateField(4, "I'll tell you how I become", "the prince of a town called Bel Air")
                };
                PgScreenData screenData = new PgScreenData("PG1", data, RefreshType.DEFAULT);
                sendScreen(screenData);
            }
        });

        pickDisplayOrientationDialogBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PgError error = pgManager.showPickDisplayOrientationDialog(SdkActivity.this);
                if (error != null) {
                    Toast.makeText(SdkActivity.this, error.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        deviceVisibilityBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                obtainDeviceVisibilityInfo();
            }
        });
    }

    private void onScannerConnectBtnClick(boolean isPinnedMode) {
        if (!pgManager.isConnectedToService()) {
            String msg = getString(R.string.connect_to_service_first);
            showMessage(msg, false);
            return;
        }

        if (pgManager.isConnectedToScanner()) {
            pgManager.disconnectScanner();
        } else if (isPinnedMode) {
            pgManager.startPairingFromPinnedActivity(this);
        } else {
            pgManager.startPairing();
        }
    }

    private void triggerFeedback() {
        boolean replaceQueueSwitchChecked = sendFeedbackWithReplaceQueueSwitch.isChecked();
        // Creating new PgCommandParams setting the queueing behaviour
        PgCommandParams params = new PgCommandParams(replaceQueueSwitchChecked);
        // Wrapping the feedback data in a PgCommand with the PgCommandData
        PgCommand<PgPredefinedFeedback> feedbackCommand = getSelectedFeedback().toCommand(params);
        pgManager.triggerFeedback(feedbackCommand, new IPgFeedbackCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Feedback successfully played.");
            }

            @Override
            public void onError(@NonNull PgError pgError) {
                final String msg = "An Error occurred during triggerFeedback: " + pgError;
                showMessage(msg, true);
            }
        });
    }

    private void changeScannerConfig(final boolean isDefault) {
        defaultFeedbackSwitch.setEnabled(false);

        PgScannerConfig config = new PgScannerConfig(isDefault);
        pgManager.setScannerConfig(config, new IPgScannerConfigCallback() {
            @Override
            public void onScannerConfigSuccess(@NonNull PgScannerConfig pgScannerConfig) {
                Log.d(TAG, "Successfully updated config on scanner");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        defaultFeedbackSwitch.setEnabled(true);
                    }
                });
            }

            @Override
            public void onError(@NonNull PgError pgError) {
                final String msg = "Could not set config on scanner: " + pgError;
                showMessage(msg, true);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        defaultFeedbackSwitch.setChecked(!isDefault);
                        defaultFeedbackSwitch.setEnabled(true);
                    }
                });
            }
        });

    }

    private void startTakingImage() {
        String imageQualityString = imageQualityET.getText().toString();
        int quality =
                imageQualityString.isEmpty() ? defaultImageQuality : Integer.parseInt(imageQualityString);

        String timeoutString = timeoutET.getText().toString();
        int timeout = timeoutString.isEmpty() ? DEFAULT_IMAGE_TIMEOUT : Integer.parseInt(timeoutString);

        PgImageConfig imageConfig = new PgImageConfig(quality, getSelectedImageResolution());

        pgManager.takeImage(imageConfig, timeout, new IPgImageCallback() {
            @Override
            public void onImageReceived(@NonNull final PgImage pgImage) {
                final Bitmap bmp = BitmapFactory.decodeByteArray(pgImage.getBytes(), 0, pgImage.getBytes().length);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageTakenIV.setImageBitmap(bmp);
                    }
                });
            }

            @Override
            public void onError(@NonNull final PgError pgError) {
                final String msg = "Taking an image failed. Error code is: " + pgError;
                showMessage(msg, true);
            }
        });

    }

    private void sendScreen(PgScreenData screenData) {
        if (pgManager.isConnectedToService() && pgManager.isConnectedToDisplay()) {
            pgManager.setScreen(screenData, new IPgSetScreenCallback() {
                @Override
                public void onSuccess() {
                    String msg = "Screen set successfully";
                    showMessage(msg, false);
                }

                @Override
                public void onError(@NonNull final PgError pgError) {
                    String msg = "Setting the screen failed. Error: " + pgError;
                    showMessage(msg, true);
                }
            });
        }
    }

    private PgPredefinedFeedback getSelectedFeedback() {
        switch (feedbackRadioGroup.getCheckedRadioButtonId()) {
            case R.id.feedbackSuccess:
                return PgPredefinedFeedback.SUCCESS;
            case R.id.feedbackError:
                return PgPredefinedFeedback.ERROR;
            case R.id.feedbackInfo:
                return PgPredefinedFeedback.SPECIAL_1;
            default:
                Log.d(TAG, "getSelectedFeedback: Nothing is selected, returning ERROR as default value.");
                return PgPredefinedFeedback.ERROR;
        }
    }

    private ImageResolution getSelectedImageResolution() {
        switch (resolutionRadioGroup.getCheckedRadioButtonId()) {
            case R.id.highResolution:
                return ImageResolution.RESOLUTION_1280_960;
            case R.id.mediumResolution:
                return ImageResolution.RESOLUTION_640_480;
            case R.id.lowResolution:
                return ImageResolution.RESOLUTION_320_240;
            default:
                return ImageResolution.values()[1];
        }
    }

    private void updateButtonStates() {
        updateServiceButtons();
        updateScannerButtons();
        updateDisplayConnectionState();
    }

    private void updateServiceButtons() {
        if (pgManager.isConnectedToService()) {
            serviceConnectBtn.setEnabled(false);
            serviceConnectBtn.setText(R.string.service_connected);
        } else {
            serviceConnectBtn.setEnabled(true);
            serviceConnectBtn.setText(R.string.connect_service);
        }
    }

    private void updateScannerButtons() {
        if (pgManager.isConnectedToService() && pgManager.isConnectedToScanner()) {
            scannerConnectBtn.setText(R.string.scanner_connected);
            scannerConnectPinnedBtn.setText(R.string.scanner_connected);
        } else {
            scannerConnectBtn.setText(R.string.pair_scanner);
            scannerConnectPinnedBtn.setText(R.string.pair_scanner);
        }
    }

    private void updateDisplayConnectionState() {
        if (pgManager.isConnectedToService() && pgManager.isConnectedToDisplay()) {
            displayStateTV.setText(R.string.display_connected);
        } else {
            displayStateTV.setText(R.string.display_disconnected);
        }
    }

    private void updateScannedResults(@NonNull BarcodeScanResults barcodeScanResults) {
        scannerResultTV.setText(barcodeScanResults.getBarcodeContent());
        String symbology = barcodeScanResults.getSymbology();
        if (symbology == null) {
            symbology = "";
        }
        resultSymbologyTV.setText(symbology);

        String msg = !symbology.isEmpty() ?
                getString(R.string.new_scan_notification, barcodeScanResults.getBarcodeContent(), symbology) :
                getString(R.string.new_scan_no_symbology_notification, barcodeScanResults.getBarcodeContent());
        showMessage(msg, false);
    }

    private void setupProfilesRecycler() {
        profilesAdapter = new ProfilesAdapter(new ArrayList<ProfileUiData>(), new ProfileClickListener() {
            @Override
            public void onProfileClicked(String profileId) {
                changeConfigProfile(profileId);
            }
        });
        profilesRecycler.setAdapter(profilesAdapter);
        profilesRecycler.setLayoutManager(new LinearLayoutManager(this));
    }

    @SuppressLint("SetTextI18n")
    private void setDefaultImageConfiguration() {
        PgImageConfig defaultImageConfig = new PgImageConfig();
        defaultImageQuality = defaultImageConfig.getJpegQuality();
        imageQualityET.setText("" + defaultImageQuality);
        timeoutET.setText("" + DEFAULT_IMAGE_TIMEOUT);
    }

    private void showMessage(final String msg, final boolean isError) {
        if (isError) {
            Log.e(TAG, msg);
        } else {
            Log.d(TAG, msg);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void changeConfigProfile(final String profileId) {
        pgManager.changeConfigProfile(
                new PgCommand<>(new PgConfigProfile(profileId)),
                new IPgConfigProfileCallback() {
                    @Override
                    public void onConfigProfileChanged(@NonNull PgConfigProfile pgConfigProfile) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(
                                        getApplicationContext(),
                                        profileId + " set successfully",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        });
                    }

                    @Override
                    public void onError(@NonNull final PgError pgError) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(
                                        getApplicationContext(),
                                        "Failed to set " + profileId + ": " + pgError.toString(),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        });
                    }
                }
        );
    }

    private void getConfigProfiles() {
        pgManager.getConfigProfiles(
                new IPgGetConfigProfilesCallback() {
                    @Override
                    public void onConfigProfilesReceived(@NonNull PgConfigProfile[] profiles) {
                        Log.d(TAG, "received " + profiles.length + " config profiles");

                        final ArrayList<ProfileUiData> uiProfiles = new ArrayList<>();
                        for (PgConfigProfile profile : profiles) {
                            uiProfiles.add(new ProfileUiData(profile.getProfileId(), profile.isActive()));
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (uiProfiles.isEmpty()) {
                                    changeProfileLabel.setVisibility(View.INVISIBLE);
                                } else {
                                    changeProfileLabel.setVisibility(View.VISIBLE);
                                }
                                profilesAdapter.updateProfiles(uiProfiles);
                            }
                        });
                    }

                    @Override
                    public void onError(@NonNull final PgError pgError) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(
                                        getApplicationContext(),
                                        "Failed to get configuration profiles: " + pgError.toString(),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        });
                    }
                }
        );
    }

    private void blockTrigger() {
        pgManager.blockPgTrigger(
                new PgCommand<>(new BlockPgTriggersParams(
                    Collections.singletonList(PredefinedPgTrigger.DefaultPgTrigger.INSTANCE),
                    Collections.singletonList(PredefinedPgTrigger.DoubleClickMainPgTrigger.INSTANCE),
                    0,
                    true)),
                new IBlockPgTriggersCallback() {
                    @Override
                    public void onBlockTriggersCommandSuccess() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(
                                        SdkActivity.this.getApplicationContext(),
                                        "Blocking trigger success",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        });
                    }

                    @Override
                    public void onError(@NonNull final PgError error) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(
                                        SdkActivity.this.getApplicationContext(),
                                        "Failed to block the trigger: " + error,
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        });
                    }
                });
    }

    // Requires Insight Mobile v1.13.0+ and Scanner v2.5.0+
    private void blockAllTriggersFor10s() {
        pgManager.blockPgTrigger(
                new PgCommand<>(new BlockPgTriggersParams(Collections.<PredefinedPgTrigger>emptyList(), Collections.<PredefinedPgTrigger>emptyList(), 10000, true)),
                new IBlockPgTriggersCallback() {
                    @Override
                    public void onBlockTriggersCommandSuccess() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(
                                    SdkActivity.this.getApplicationContext(),
                                    "Blocking all triggers success",
                                    Toast.LENGTH_LONG
                                ).show();
                            }
                        });
                    }

                    @Override
                    public void onError(@NonNull final PgError error) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(
                                    SdkActivity.this.getApplicationContext(),
                                    "Failed to block all triggers: " + error,
                                    Toast.LENGTH_LONG
                                ).show();
                            }
                        });
                    }
                });
    }

    private void unblockTrigger() {
        pgManager.blockPgTrigger(
                new PgCommand<>(new BlockPgTriggersParams(Collections.<PredefinedPgTrigger>emptyList(), Collections.<PredefinedPgTrigger>emptyList(), 0, false)),
                new IBlockPgTriggersCallback() {
                    @Override
                    public void onBlockTriggersCommandSuccess() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(
                                        SdkActivity.this.getApplicationContext(),
                                        "Unblocking triggers success",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        });
                    }

                    @Override
                    public void onError(@NonNull final PgError error) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(
                                        SdkActivity.this.getApplicationContext(),
                                        "Failed to unblock the trigger: " + error,
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        });
                    }
                });
    }

    private void obtainDeviceVisibilityInfo() {
        pgManager.obtainDeviceVisibilityInfo(new IPgDeviceVisibilityCallback() {
            @Override
            public void onError(final PgError error) {
                // Handle error
                logger.log(Level.SEVERE, "Error during obtainDeviceVisibilityInfo: " + error.toString());

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder builder = new AlertDialog.Builder(SdkActivity.this);

                        builder.setTitle(R.string.device_visibility_alert_title);
                        builder.setMessage(getString(
                                R.string.device_visibility_alert_content_error, error
                        ));
                        builder.create().show();
                    }
                });
            }

            @Override
            public void onDeviceVisibilityInfoObtained(@NonNull final DeviceVisibilityInfo deviceVisibilityInfo) {
                // content of deviceVisibilityInfo
                logger.log(Level.INFO, "deviceVisibilityInfo: " + deviceVisibilityInfo.toString());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder builder = new AlertDialog.Builder(SdkActivity.this);

                        builder.setTitle(R.string.device_visibility_alert_title);
                        builder.setMessage(getString(R.string.device_visibility_alert_content,
                                deviceVisibilityInfo.getSerialNumber(),
                                deviceVisibilityInfo.getFirmwareRevision(),
                                deviceVisibilityInfo.getBatteryLevel(),
                                deviceVisibilityInfo.getBceRevision(),
                                deviceVisibilityInfo.getModelNumber(),
                                deviceVisibilityInfo.getManufacturer(),
                                deviceVisibilityInfo.getAppVersion()));
                        builder.create().show();
                    }
                });
            }
        });
    }
}

interface ProfileClickListener {
    void onProfileClicked(String profileId);
}

/**
 * Profile data for displaying on UI.
 */
final class ProfileUiData {
    String profileId;
    Boolean active;

    ProfileUiData(String profileId, Boolean active) {
        this.profileId = profileId;
        this.active = active;
    }
}
