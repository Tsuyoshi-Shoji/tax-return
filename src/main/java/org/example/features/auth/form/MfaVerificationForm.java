package org.example.features.auth.form;

public class MfaVerificationForm {
    private String code;
    private String backupCode;
    private boolean useBackupCode = false;

    public MfaVerificationForm() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getBackupCode() {
        return backupCode;
    }

    public void setBackupCode(String backupCode) {
        this.backupCode = backupCode;
    }

    public boolean isUseBackupCode() {
        return useBackupCode;
    }

    public void setUseBackupCode(boolean useBackupCode) {
        this.useBackupCode = useBackupCode;
    }
}
