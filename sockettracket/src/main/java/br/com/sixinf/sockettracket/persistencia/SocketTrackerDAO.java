/**
 * 
 */
package br.com.sixinf.sockettracket.persistencia;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.apache.log4j.Logger;

import br.com.sixinf.ferramentas.dao.BridgeBaseDAO;
import br.com.sixinf.ferramentas.dao.HibernateBaseDAOImp;
import br.com.sixinf.ferramentas.persistencia.AdministradorPersistencia;

/**
 * @author maicon
 *
 */
public class SocketTrackerDAO extends BridgeBaseDAO {
	
	private static final Logger log = Logger.getLogger(SocketTrackerDAO.class);
	
	private static SocketTrackerDAO dao;
	
	public static SocketTrackerDAO getInstance(){
		if (dao == null)
			dao = new SocketTrackerDAO();
		return dao;
	}
	
	public SocketTrackerDAO() {
		super(new HibernateBaseDAOImp());
	}
	
	public List<Tracker> buscarTodosTrackersAtivos() {
		EntityManager em = AdministradorPersistencia.getEntityManager();
		
		List<Tracker> list = null;
		try {
			StringBuilder hql = new StringBuilder();
			hql.append("select t from Tracker t ");
			hql.append("where t.statusRegistro = 'A' ");
			TypedQuery<Tracker> q = em.createQuery(hql.toString(), Tracker.class);
			
			list = q.getResultList();
						
		} catch (Exception e) {
			log.error("Erro ao buscar Playlist", e);
		} finally {
            em.close();
        }
		return list;
	}
	
	public Tracker buscarTodosTrackersPeloSerial(String serial) {
		EntityManager em = AdministradorPersistencia.getEntityManager();
		
		Tracker t = null;
		try {
			StringBuilder hql = new StringBuilder();
			hql.append("select t from Tracker t ");
			hql.append("where t.statusRegistro = 'A' ");
			hql.append("and t.numeroSerie = :numeroSerie ");
			TypedQuery<Tracker> q = em.createQuery(hql.toString(), Tracker.class);
			q.setMaxResults(1);
			q.setParameter("numeroSerie", serial);
						
			t = q.getSingleResult();
						
		} catch (NoResultException e) {
        	
        } catch (Exception e) {
			log.error("Erro ao buscar Playlist", e);
		} finally {
            em.close();
        }
		return t;
	}
	
}
