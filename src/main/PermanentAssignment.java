public class PermanentAssignment extends AbstractRoleAssignment {
    private boolean revoked = false;

    public PermanentAssignment(User user, Role role, AssignmentMetadata assignmentMetadata) {
        super(user, role, assignmentMetadata);
    }

    @Override
    public String assignmentType() {
        return "PERMANENT";
    }

    @Override
    public boolean isActive() {
        return !revoked;
    }

    public void revoke() {
        if (revoked) {
            throw new IllegalStateException("Assignment already revoked");
        }
        this.revoked = true;
    }

    public boolean isRevoked() {
        return revoked;
    }
}