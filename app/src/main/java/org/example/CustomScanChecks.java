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
    private Set<String> excludedMethods;
    private Set<String> listParam;

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        api.extension().setName("Exclude Methods in Scanner");

        // Create a custom tab for user input
        SwingUtilities.invokeLater(() -> {
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            JPanel inputPanel = new JPanel(new GridBagLayout());
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

            JTextField methodTextField = new JTextField();
            methodTextField.setPreferredSize(new Dimension(200, 30));
            JTextField paramTextField = new JTextField();
            paramTextField.setPreferredSize(new Dimension(200, 30));

            JButton button = new JButton("Set Excluded");

            button.addActionListener(e -> {
                String methodInput = methodTextField.getText().trim().toUpperCase();
                excludedMethods = new HashSet<>(Arrays.asList(methodInput.split(",")));

                listParam = new HashSet<>(Arrays.asList(paramTextField.getText().trim().split("&")));

                JOptionPane.showMessageDialog(panel, "Excluded methods set to: " + excludedMethods +
                        "\nParameters set to: " + listParam);
            });

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 5, 5, 5);

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 0.1;
            inputPanel.add(new JLabel("Excluded Methods:"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            inputPanel.add(methodTextField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.weightx = 0.1;
            inputPanel.add(new JLabel("Parameter Name:"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            inputPanel.add(paramTextField, gbc);

            buttonPanel.add(button);

            panel.add(inputPanel, BorderLayout.NORTH);
            panel.add(buttonPanel, BorderLayout.SOUTH);

            api.userInterface().registerSuiteTab("Exclude Methods in Scanner", panel);
        });

        api.http().registerHttpHandler(new HttpHandler() {
            @Override
            public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
                if (check(requestToBeSent)) {
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

    public boolean check(HttpRequestToBeSent requestToBeSent) {
        if (requestToBeSent.toolSource().isFromTool(ToolType.SCANNER)) {
            return false;
        }
        if (excludedMethods.contains(requestToBeSent.method().toUpperCase())) {
            return true;
        }
        // Check if the request has the specified parameter with the specified value
        for (String param : listParam) {
            // split param into key and value, split by '=', name is first, value is the
            // other left
            String name, value;
            int index = param.indexOf("=");
            if (index == -1) {
                name = param;
                value = "";
            } else {
                name = param.substring(0, index);
                value = param.substring(index + 1);
            }
            // check if the request has the specified parameter with the specified value
            if (requestToBeSent.parameters().stream()
                    .anyMatch(p -> p.name().equals(name) && p.value().equals(value))) {
                return true;
            }
        }
        return false;
    }
}