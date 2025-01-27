package org.example;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.BurpExtension;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.handler.*;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CustomScanChecks implements BurpExtension {
    private MontoyaApi api;
    private Set<String> excludedMethods = new HashSet<>();

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        api.extension().setName("Exclude Methods in Scanner");
        SwingUtilities.invokeLater(() -> {
            JPanel panel = new JPanel(new BorderLayout());
            JTextField textField = new JTextField(String.join(",", excludedMethods));
            JButton button = new JButton("Set Excluded Methods");

            button.addActionListener(e -> {
                String input = textField.getText().trim().toUpperCase();
                excludedMethods = new HashSet<>(Arrays.asList(input.split(",")));
                JOptionPane.showMessageDialog(panel, "Excluded methods set to: " + excludedMethods);
            });

            panel.add(textField, BorderLayout.PAGE_START);
            panel.add(button, BorderLayout.AFTER_LAST_LINE);

            api.userInterface().registerSuiteTab("Exclude Methods in Scanner", panel);
        });

        api.http().registerHttpHandler(new HttpHandler() {
            @Override
            public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
                if (requestToBeSent.toolSource().isFromTool(ToolType.SCANNER)
                        && excludedMethods.contains(requestToBeSent.method().toUpperCase())) {
                    return RequestToBeSentAction
                            .continueWith(requestToBeSent.withBody("").withPath("/VCS404").withMethod("VCS"));
                }
                return RequestToBeSentAction.continueWith(requestToBeSent);
            }

            @Override
            public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
                return ResponseReceivedAction.continueWith(responseReceived);
            }
        });
    }
}