package net.lightapi.bot.agent;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

public class Utility {
    public static boolean hasAtLeastOneReference(Repository repo) {
        for (Ref ref : repo.getAllRefs().values()) {
            if (ref.getObjectId() == null)
                continue;
            return true;
        }
        return false;
    }
}
