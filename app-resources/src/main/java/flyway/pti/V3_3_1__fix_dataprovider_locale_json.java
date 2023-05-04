package flyway.pti;

import fi.nls.oskari.util.JSONHelper;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Some locales are saved as { fi: {...} } which doesn't work with Postgres JSON-functions
 * Pass them through JSONObject to add quotes and fix this to { "fi": {...}}
 */
public class V3_3_1__fix_dataprovider_locale_json extends BaseJavaMigration {
    class Result {
        int id;
        String locale;
    }

    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        List<Result> layers = getDataToModify(connection);
        layers.forEach(r -> {
            JSONObject value = JSONHelper.createJSONObject(r.locale);
            if (value != null) {
                r.locale = value.toString();
            }
        });
        saveChanges(connection, layers);
    }

    public List<Result> getDataToModify(Connection conn) throws Exception {
        List<Result> layers = new ArrayList<>();
        final String sql = "SELECT id, locale FROM oskari_dataprovider";
        try(PreparedStatement statement = conn.prepareStatement(sql)) {
            try (ResultSet rs = statement.executeQuery()) {
                while(rs.next()) {
                    Result result = new Result();
                    result.id = rs.getInt("id");
                    result.locale = rs.getString("locale");
                    layers.add(result);
                }
            }
        }
        return layers;
    }

    private void saveChanges(Connection conn, List<Result> results) throws SQLException {
        String sql = "UPDATE oskari_dataprovider SET locale = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Result r : results) {
                ps.setString(1, r.locale);
                ps.setInt(2, r.id);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
