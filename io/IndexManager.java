package oxygen.io;

/*
 * 索引管理模块技术规格说明书 version 1.0
 * 概述索引模块的实现目标和实现方法以及技术规格。
 * 
 * 一、实现目标
 * 由于数据库文件的庞大数据量，不可能将其全部放入内存。而由于磁盘存取和内存存取速度的巨大差异，全部使用磁盘I/O存取数据
 * 将严重影响性能。为此，引用了索引技术。索引的目的是最大可能的减少磁盘I/O次数，将磁盘I/O对性能的影响降到最低。
 * 对一个实体而言，其索引字段应该是用于查询的字段。将查询的字段标注为索引字段，那么，以该字段为条件进行实体查询的时候，
 * 首先该字段会通过散列函数形成一个long型数值，通过该数值可以从映射表直接得到实体在实体文件中的偏移量。若没有对查询字段
 * 标注索引，那么，将不得不依次搜索实体文件中的每个实体，性能是非常低下的。
 * 
 * 
 * 二、目录结构
 * 索引文件是后缀名为".idx"的文件，位于数据库data/index的子目录下。具体路径由索引实体名和字段名给出，
 * 例如：实体User中的name字段为一个索引字段，则路径为data/index/User/name.idx
 * 若User类属于包xxx.yyy.zzz则路径为data/index/xxx.yyy.zzz.User/name.idx
 * 
 * 
 * 三、文件结构
 * 索引文件的文件全部由单条长度为16字节的索引(index)构成，因此，第一条索引的偏移量为0，第二条为16，第三条为32……
 * 第N条为16*(N-1)。每一条索引又分为键(key)和值(value)，键处于索引的高8位，值处于索引的低8位。
 * 每一个键和值都可以被作为一个Java的long型基本数据类型读出。键由索引的字符串根据散列函数计算得出，
 * 而值由待索引的实体在实体文件中的偏移量给出。这样，根据索引的字符串，就能快速地找到实体在实体文件中的位置，
 * 而不用搜索整个实体文件，大大减少了磁盘I/O次数。
 * 例如：
 * 实体User的name字段为一个索引字段，有一个实体(entity)的name字段值为"Susan"，根据散列函数，
 * 计算出键为"349082820474922L"，而该实体在实体文件中的存储位置是3832920238L，那么，
 * 键值对"349082820474922L,3832920238L"构成了一条索引。每当需要查询"Susan"时，计算出散列值，
 * 也就是键为"349082820474922L"，查询到该键对应的值为"3832920238L"，即可在实体文件的3832920238L位置处找到对应的实体。
 * 注意：
 * 1、被索引的字段是可以重复的，即实体文件中可以有多个实体的name字段都为"Susan"，在这种情况下，
 * 将有多条索引的键满足要求。返回的值将是一个集合。具体请看第四部分”数据结构“。
 * 2、在相当特殊的情况下，有另一个字符串，假设为"Julie"，它的散列值和"Susan"相同，都为349082820474922L，
 * 那么，当查询"Susan"时，将同时查询到"Julie"的地址。返回的值将是一个同时包含"Susan"和"Julie"的集合，
 * 因此，根据地址在实体文件中查询时，应该对实体的索引字符串进行验证。（基于发生此情况的概率非常非常小，本版本忽略这种情况的发生）
 * 
 * 
 * 四、数据结构
 * 在目前这个版本中，索引文件是准备全部读入到内存的（这样做的问题是，若记录非常多，则需要更多的内存空间，
 * 1MB的索引文件能够容纳的索引数目是65536条）。实体文件偏移量映射表entityMap<Long, Object>将容纳键（索引字符串的hash值）和
 * 值（实体文件中的偏移量）的映射关系。由于值可能是单独的一个值，也可能是一个集合，所以返回的Object可能是
 * 一个Long对象，也可能是一个HashSet<Long>对象。
 * 例如：
 * 查询一个索引字段name为"Susan"的实体，若仅有一个实体的name字段为"Susan"，那么，返回的将是这个实体在实体文件中
 * 偏移量的Long对象，若有多个实体符号条件，则返回一个HashSet<Long>包含了所有的符合条件实体在实体文件中的偏移量。
 * 再强调一点：
 * 由于散列冲突的存在，返回的值或者集合都不是绝对可靠的，需要在实体文件中对索引字段进行验证。
 * 
 * 
 * 五、存取方法
 * 实体的索引字段和索引文件是一一对应的。如User类的name字段为索引字段，那么，对基于该索引的增删查改行为都在
 * data/index/User/name.idx上进行（参见第一部分”目录结构“），文件data/index/User/name.idx也是该索引的唯一依据。
 * 当一次存取操作失败时，应当将错误信息写入到日志文件，并提供尽可能详细的信息。每一个操作都应该返回一个标志，
 * 用于判断操作是否成功完成。
 * 
 * 1、索引文件的读取
 * 在目前这个版本中，索引文件将被全部装入内存，因此，不考虑内存溢出的问题。磁盘I/O读入的索引文件将被划分成16字节的一个个索引，
 * 保存在实体文件偏移量映射表entityMap<Long, Object>中，若读入的索引键没有重复，那么参数类型将是<Long, Long>形式；反之，
 * 参数类型将是<Long, HashSet<Long>>的形式。为便于将来的增删查改工作，另设一个索引文件偏移量映射表
 * indexMap<Long, Integer>（这里之所以没有使用Long而是Integer，是基于索引文件不大于4GB的一个假设，
 * 4GB的索引文件能够保存约2.7亿条记录。由于这个版本将索引文件全部装入内存，即使在高端服务器上，一次装入4GB的索引对资源的
 * 消耗也是惊人的，所以使用Integer足够），这个映射表用于保存实体文件中实体偏移量与实体中该字段索引在索引文件中偏移量的映射。
 * 例如：
 * 实体User中有一个索引字段name，在索引文件data/index/User/name.idx中某一条偏移量为7485948的索引为
 * 键值对"349082820474922L,3832920238L"，那么，除了将该键值对加入到entityMap中，另外还需要将
 * 键值对"3832920238L, 7485948"加入到indexMap中。这样做的好处是，当删除实体文件中偏移量为3832920238L的实体时，
 * 可以方便的在索引文件偏移量为7485948的地方删除它的索引，而不是挨个在索引文件中寻找该实体对应的索引。
 * 另外，在读入索引时，若读入的键和值都为0L，那么这样的索引是一个空索引，它是由于索引删除后还没有添加新索引而产生的。
 * 空索引放置于一个空位集合freeSet<Integer>中，当执行添加操作时，优先选择插入到空位集合中的空位中；
 * 删除操作中，被删除索引在索引文件中的偏移量也将被添加到空位集合中。
 * 
 * 2、插入(insert)索引
 * 首先寻找索引文件中是否有删除索引后留下的空位（想象一个连续的索引文件，中间某条索引被删除后，留下的空位），
 * 这个空位由空位集合freeSet<Integer>提供。若空位集合能够提供一个空位，那么新索引将被写到这个空位当中；
 * 若空位集合无法提供空位，那么新索引将被写入到文件末尾。当写入索引到索引文件成功后，在entityMap和indexMap里增加新的索引映射。
 * 若由于任何原因，写入索引到文件的操作失败，那么都不往entityMap和indexMap增加新的索引映射。
 * 
 * 3、查询(query)索引
 * 直接在entityMap中查找。返回空的HashSet<Long>对象，表示没有这个索引，数据库中不存在带这个索引的实体（这一点是确定的，当一个实体被插入
 * 到数据库时，其所有的索引字段都将被作为新索引插入到索引文件中）；返回有内容的HashSet<Long>对象，
 * 表示有对象满足查询索引，这个HashSet<Long>包含了所有满足条件的实体在实体文件中的偏移量。
 * 
 * 4、删除(delete)索引
 * 删除索引的操作仅发生在实体的删除时。由于在读取索引时，保存了索引偏移量映射表indexMap，因此对查询索引偏移量提供了很大的方便。
 * 删除实体时，必然先通过查询索引得到实体在实体文件中的偏移量，该偏移量通过indexMap映射到实体索引在索引文件的偏移量，
 * 找到了索引的偏移量，就能方便的删除索引（将键和值清为0L）。在文件中删除索引成功后，将其索引偏移量添加到空位集合
 * freeSet中，等待插入索引时可以插入到这个空位。
 * 
 * 5、修改(update)索引
 * 要修改索引首先要查询到该索引，确定该索引在索引文件中的偏移量，然后向该偏移量写入新的键和值。索引文件修改成功后，要更新
 * entityMap和indexMap。若索引文件修改失败，则不对entityMap和indexMap进行任何修改。
 * 
 * 
 * 六、多线程、回滚技术和保护索引
 * 对索引的操作都是由索引加载器完成的，并且多个操作同一索引的线程使用同一个索引加载器。因此，索引加载器必须是多线程安全的。
 * 索引加载器中保存索引文件中间状态的映射表和集合等也应该是线程安全的。
 * 由于微事务的存在，因此索引也必须使用回滚技术（更多有关微事务和回滚技术的概念，请参看Handler类中的“实体数据访问处理模块
 * 技术规格说明书”）。索引的微事务是使用定义在实体数据访问处理器中的回滚队列来表达的，一个回滚队列标示一个微事务。当索引
 * 插入到索引文件成功后，若该索引操作属于一个微事务的话，那么索引不能立刻生效，因为事务还没有结束。索引将首先生成一个
 * 可以恢复索引操作的索引回滚类并放入到回滚队列中，然后生成一个保护索引类放到索引加载器的保护索引集合映射表中，由回滚
 * 队列来映射。当微事务成功完成，索引将解除保护，并立刻生效；若失败，保护索引将被删除，其对索引文件的修改也将通过回滚
 * 而恢复。当索引操作不属于一个微事务时，那么这个索引是“自由”的，只要成功写入到索引文件，就立刻生效。
 * 上面提到了一个“保护索引”的概念：由于索引写入到文件，而微事务尚未结束，所以必须对写入文件的索引进行保护，以避免其他
 * 线程在微事务没有结束的情况下就对索引进行修改，造成数据的不一致。对索引进行保护是在索引属于一个微事务的前提下进行的，
 * 在索引修改索引文件之后，微事务完成之前，索引都处于保护状态，索引加载器确保该索引不会被任何线程所修改（包括对索引
 * 进行保护的线程）。对索引进行操作前都应该调用索引加载器的waitIfProtected方法，如果索引处于保护状态，那么就等待其
 * 保护状态结束。
 * 索引的回滚技术是基于实体回滚技术的，参看Handler类的“实体数据访问处理模块技术规格说明书”的回滚技术部分和多线程部分可以
 * 获得更完整的理解。
 */

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import oxygen.util.i18n.ResourceLoader;
import oxygen.util.i18n.ResourceLoaderProvider;


/**
 * <p>索引管理器</p>
 * <p>索引管理器提供了{@link Handler 实体数据访问处理器}操作索引的接口。实际上索引管理器相当于
 * {@link IndexLoader 索引加载器}的代理，每一个对索引的操作请求都将在索引加载器中完成。</p>
 * @author 赖昆
 * @since 1.0, 2007-01-10
 * @version 1.0
 * @see Handler 实体数据访问处理器
 * @see IndexLoader 索引加载器
 */
public class IndexManager {
	
	// 索引加载器的映射表
	private static final Map<Field, IndexLoader> loaderMap = new ConcurrentHashMap<Field, IndexLoader>();
	
	// 微事务-索引加载器集合映射表：标示微事务涉及的索引加载器。每个微事务以一个与之对应的回滚队列表示。
	private static final Map<MicroTransaction, Collection<IndexLoader>> transactionMap = new ConcurrentHashMap<MicroTransaction, Collection<IndexLoader>>();
	
	private static final Logger logger = Logger.getLogger( IndexManager.class.getName() );
	
	private static final ResourceLoader res = ResourceLoaderProvider.provide( IndexManager.class );
	
	// 获得索引字段对应的索引加载器
	private synchronized static IndexLoader getLoader( Field field ) {
		
		// 若该索引字段没有对应的索引加载器，则新生成一个，并存放到索引加载器映射表
		IndexLoader loader = loaderMap.get( field );
		if ( loader == null ) {
			try {
				loader = new IndexLoader( field );
				loaderMap.put( field, loader );
			} catch ( IOException e ) {
				logger.warning( res.getResource( "IndexManager.getLoader.warning.CannotOpenIndexFile", field.getDeclaringClass().getSimpleName(), field.getName() ) );
				return null;
			}
		}

		return loader;
	}
	
	// 当索引操作属于一个微事务时，记录此微事务涉及到的索引加载器，以便微事务结束（成功或失败）时释放
	private static void preHandleTransaction( MicroTransaction transaction, IndexLoader loader ) {

		if ( transaction != null ) {
			Collection<IndexLoader> c = transactionMap.get( transaction );
			if ( c == null ) {
				c = new HashSet<IndexLoader>();
				transactionMap.put( transaction, c );
			}
			if ( !c.contains( loader ) ) {
				c.add( loader );
			}
		}
	}

	/**
	 * 插入一条索引
	 * @param field 带索引的实体字段
	 * @param key 实体字段的字符串值
	 * @param address 实体地址
	 * @param transaction 微事务（若插入索引操作不属于一个微事务，则为null）
	 * @return 成功插入索引，返回true；反之返回false
	 */
	public static boolean insert( Field field, String key, long address, MicroTransaction transaction ) {

		IndexLoader loader = getLoader( field );
		if ( loader == null ) return false;
		
		preHandleTransaction( transaction, loader );
		
		return loader.insert( IndexHasher.hash( key ), address, transaction );
	}
	
	/**
	 * 查询索引
	 * @param field 带索引的实体字段
	 * @param key 实体字段的字符串值
	 * @return 若查询成功，返回所有满足查询条件的实体地址，没有查询到满足条件的实体返回一个空集合；若查询失败，返回null
	 */
	public static HashSet<Long> query( Field field, String key ) {
		
		IndexLoader loader = getLoader( field );
		if ( loader == null ) {
			logger.warning( res.getResource( "IndexManager.query.warning.CannotLoadIndex", field.getDeclaringClass().getSimpleName(), field.getName() ) );
			throw new IllegalStateException( res.getResource( "IndexManager.query.warning.CannotLoadIndex", field.getDeclaringClass().getSimpleName(), field.getName() ) );
		}
		return loader.query( IndexHasher.hash( key ) );
	}
	
	/**
	 * 删除一条索引
	 * @param field 带索引的实体字段
	 * @param address 实体地址
	 * @param transaction 微事务（若删除索引操作不属于一个微事务，则为null）
	 * @return 成功删除索引，返回true；反之返回false
	 */
	public static boolean delete( Field field, long address, MicroTransaction transaction ) {
		
		IndexLoader loader = getLoader( field );
		if ( loader == null ) return false;
		
		preHandleTransaction( transaction, loader );
		
		return loader.delete( address, transaction );
	}
	
	/**
	 * 修改一条索引
	 * @param field 带索引的实体字段
	 * @param key 实体字段的字符串值
	 * @param address 实体地址
	 * @param transaction 微事务（若修改索引操作不属于一个微事务，则为null）
	 * @return 成功修改索引，返回true；反之返回false
	 */
	public static boolean update( Field field, String key, long address, MicroTransaction transaction ) {
		
		IndexLoader loader = getLoader( field );
		if ( loader == null ) return false;
		
		preHandleTransaction( transaction, loader );
		
		return loader.update( IndexHasher.hash( key ), address, transaction );
	}
	
	// 解除微事务中对索引的保护
	static void unprotect( MicroTransaction transaction, boolean isSuccessful ) {
		
		Collection<IndexLoader> c = transactionMap.get( transaction );
		if ( c != null ) {
			for ( IndexLoader loader : c ) {
				loader.unprotect( transaction.getKeepsake(), isSuccessful );
			}
			transactionMap.remove( transaction );
		}
	}
	
	/**
	 * 安全关闭所有索引加载器
	 */ 
	public synchronized static void close() {

		// 依次关闭索引加载器
		for ( IndexLoader loader : loaderMap.values() ) {
			loader.close();
		}
		loaderMap.clear();
	}
}
