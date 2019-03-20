package flyway.olcesium;

import fi.nls.oskari.domain.map.view.View;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.view.AppSetupServiceMybatisImpl;
import fi.nls.oskari.map.view.ViewService;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class V1_2__3D_index_page implements JdbcMigration {

    private static final Logger LOG = LogFactory.getLogger(V1_2__3D_index_page.class);
    private static final String CESIUM_VIEW_NAME = "Geoportal Ol Cesium";
    private static final String VIEW_3D_PAGE = "index3D";

    private ViewService viewService = null;

    public void migrate(Connection connection) throws SQLException {
        viewService =  new AppSetupServiceMybatisImpl();
        updateCesiumViews(connection);
    }

    private void updateCesiumViews(Connection connection) {
        try {
            List<Long> viewIds = getCesiumViewIds(connection);
            LOG.info("Updating Cesium views - count:", viewIds.size());

            for(Long viewId : viewIds) {
                View modifyView = viewService.getViewWithConf(viewId);
                modifyView.setPage(VIEW_3D_PAGE);
                viewService.updateView(modifyView);
            }
        } catch (Exception e) {
            LOG.error(e, "Error setting new index page for cesium views");
        }
    }

    private List<Long> getCesiumViewIds(Connection conn) throws SQLException {
        List<Long> list = new ArrayList<>();
        final String sql = "SELECT id FROM portti_view WHERE name=?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, CESIUM_VIEW_NAME);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getLong("id"));
                }
            }
        } catch (Exception e) {
            LOG.error(e, "Error getting Cesium portti views");
        }
        return list;
    }
}
