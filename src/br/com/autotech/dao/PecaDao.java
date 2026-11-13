package br.com.autotech.dao;

import br.com.autotech.exception.PersistenciaException;
import br.com.autotech.factory.ConexaoFactory;
import br.com.autotech.model.Peca;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PecaDao {

    public void criarTabelaSeNaoExistir() {
        String sql = """
            CREATE TABLE IF NOT EXISTS pecas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                codigo TEXT UNIQUE NOT NULL,
                nome TEXT NOT NULL,
                preco REAL NOT NULL,
                estoque INTEGER NOT NULL
            );
        """;

        try (Connection conn = ConexaoFactory.obterConexao();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sql);
            System.out.println("🔧 [AutoTech] Tabela 'pecas' verificada/criada com sucesso!");

        } catch (SQLException e) {
            System.err.println("[LOG JDBC] Código: " + e.getErrorCode() + " - " + e.getMessage());
            throw new PersistenciaException("Falha ao criar tabela pecas no banco de dados local.", e);
        }
    }

    public Peca salvar(Peca peca) {
        String sql = "INSERT INTO pecas (codigo, nome, preco, estoque) VALUES (?, ?, ?, ?);";

        try (Connection conn = ConexaoFactory.obterConexao();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, peca.getCodigo());
            stmt.setString(2, peca.getNome());
            stmt.setDouble(3, peca.getPreco());
            stmt.setInt(4, peca.getEstoque());

            int linhas = stmt.executeUpdate();
            if (linhas > 0) {
                try (ResultSet chaves = stmt.getGeneratedKeys()) {
                    if (chaves.next()) {
                        long idGerado = chaves.getLong(1);
                        peca.setId(idGerado);
                        System.out.println("✅ Peça gravada com sucesso! ID gerado pelo banco: " + idGerado);
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("[LOG JDBC] Código: " + e.getErrorCode() + " - " + e.getMessage());
            throw new PersistenciaException("Falha ao salvar a peça no banco de dados.", e);
        }

        return peca;
    }

    public List<Peca> listarTodas() {
        List<Peca> lista = new ArrayList<>();
        String sql = "SELECT id, codigo, nome, preco, estoque FROM pecas ORDER BY nome;";

        try (Connection conn = ConexaoFactory.obterConexao();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Peca p = new Peca(
                    rs.getLong("id"),
                    rs.getString("codigo"),
                    rs.getString("nome"),
                    rs.getDouble("preco"),
                    rs.getInt("estoque")
                );
                lista.add(p);
            }

        } catch (SQLException e) {
            System.err.println("[LOG JDBC] Código: " + e.getErrorCode() + " - " + e.getMessage());
            throw new PersistenciaException("Falha ao listar peças do banco de dados.", e);
        }

        return lista;
    }

    public Optional<Peca> buscarPorCodigo(String codigo) {
        String sql = "SELECT id, codigo, nome, preco, estoque FROM pecas WHERE codigo = ?;";

        try (Connection conn = ConexaoFactory.obterConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, codigo);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Peca peca = new Peca(
                        rs.getLong("id"),
                        rs.getString("codigo"),
                        rs.getString("nome"),
                        rs.getDouble("preco"),
                        rs.getInt("estoque")
                    );
                    return Optional.of(peca);
                }
            }

        } catch (SQLException e) {
            System.err.println("[LOG JDBC] Código: " + e.getErrorCode() + " - " + e.getMessage());
            throw new PersistenciaException("Falha ao buscar a peça por código.", e);
        }

        return Optional.empty();
    }

    public boolean darBaixaEstoque(Long id, int quantidadeRetirada) {
        String sql = "UPDATE pecas SET estoque = estoque - ? WHERE id = ? AND estoque >= ?;";

        try (Connection conn = ConexaoFactory.obterConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, quantidadeRetirada);
            stmt.setLong(2, id);
            stmt.setInt(3, quantidadeRetirada);

            int linhas = stmt.executeUpdate();
            return linhas > 0;

        } catch (SQLException e) {
            System.err.println("[LOG JDBC] Código: " + e.getErrorCode() + " - " + e.getMessage());
            throw new PersistenciaException("Falha ao atualizar o estoque da peça.", e);
        }
    }
}
