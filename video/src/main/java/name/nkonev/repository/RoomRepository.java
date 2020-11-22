package name.nkonev.repository;

import name.nkonev.entity.RoomEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

// todo change to file json, because redis doesn't need
//  although maybe need for the replication
@Repository
public interface RoomRepository extends CrudRepository<RoomEntity, Long> {

}
