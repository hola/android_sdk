package org.hola.cdn_sdk;
import android.net.Uri;
import com.google.android.exoplayer.upstream.DataSpec;
import com.google.android.exoplayer.upstream.HttpDataSource;
import java.util.List;
import java.util.Map;
class exoplayer_data_source implements HttpDataSource {
private final HttpDataSource m_source;
private DataSpec m_dataspec;
private DataSpec m_orig_dataspec;
exoplayer_data_source(HttpDataSource source){
    this.m_source = source; }
@Override
public long open(DataSpec dataSpec) throws HttpDataSourceException {
    this.m_orig_dataspec = dataSpec;
    if (!this.m_orig_dataspec.uri.getHost().equals("127.0.0.1"))
    {
        this.m_dataspec = new DataSpec(
            Uri.parse(service.mangle_request(dataSpec.uri)), dataSpec.postBody,
            dataSpec.absoluteStreamPosition, dataSpec.position, dataSpec.length,
            dataSpec.key, dataSpec.flags);
    }
    else
        this.m_dataspec = m_orig_dataspec;
    return m_source.open(this.m_dataspec);
}
@Override
public void close() throws HttpDataSourceException { m_source.close(); }
@Override
public int read(byte[] bytes, int i, int i1) throws HttpDataSourceException {
    return m_source.read(bytes, i, i1); }
@Override
public void setRequestProperty(String s, String s1){
    m_source.setRequestProperty(s, s1); }
@Override
public void clearRequestProperty(String s){
    m_source.clearRequestProperty(s); }
@Override
public void clearAllRequestProperties(){
    m_source.clearAllRequestProperties(); }
@Override
public Map<String, List<String>> getResponseHeaders(){
    return m_source.getResponseHeaders(); }
@Override
public String getUri(){ return m_orig_dataspec.uri.toString(); }
}
